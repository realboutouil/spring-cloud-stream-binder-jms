package com.boutouil.binder.jms.message.handler;

import com.boutouil.binder.jms.provision.JmsConsumerDestination;
import org.springframework.core.AttributeAccessor;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.context.OrderlyShutdownCapable;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.support.DefaultErrorMessageStrategy;
import org.springframework.integration.support.ErrorMessageUtils;
import org.springframework.jms.config.AbstractJmsListenerContainerFactory;
import org.springframework.jms.config.SimpleJmsListenerEndpoint;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.support.JmsHeaders;
import org.springframework.jms.support.JmsUtils;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.HeaderMapper;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class JmsInboundChannelAdapter extends MessageProducerSupport implements OrderlyShutdownCapable {

    private static final ThreadLocal<AttributeAccessor> ATTRIBUTES_HOLDER = new ThreadLocal<>();

    //constructor args
    private final AbstractJmsListenerContainerFactory<DefaultMessageListenerContainer> listenerContainerFactory;
    private final JmsConsumerDestination jmsConsumerDestination;
    private final String group;
    private final MessageConverter messageConverter;
    private final HeaderMapper<Message> headerMapper;

    //Create on init
    private AbstractMessageListenerContainer messageListenerContainer;
    private RetryTemplate retryTemplate;
    private RecoveryCallback<?> recoveryCallback;

    //Can be configed via setters
    private boolean bindSourceMessage = false;

    public JmsInboundChannelAdapter(
            AbstractJmsListenerContainerFactory<DefaultMessageListenerContainer> listenerContainerFactory,
            JmsConsumerDestination jmsConsumerDestination,
            String group,
            MessageConverter messageConverter,
            HeaderMapper<Message> headerMapper) {
        this.listenerContainerFactory = listenerContainerFactory;
        this.jmsConsumerDestination = jmsConsumerDestination;
        this.group = group;
        this.messageConverter = messageConverter;
        this.headerMapper = headerMapper;
        setErrorMessageStrategy(new DefaultErrorMessageStrategy());
    }

    public void setRetryTemplate(RetryTemplate retryTemplate) {
        this.retryTemplate = retryTemplate;
    }

    public void setRecoveryCallback(RecoveryCallback<?> recoveryCallback) {
        this.recoveryCallback = recoveryCallback;
    }

    public void setBindSourceMessage(boolean bindSourceMessage) {
        this.bindSourceMessage = bindSourceMessage;
    }

    @Override
    public String getComponentType() {
        return "jms:inbound-channel-adapter";
    }

    @Override
    protected void onInit() {
        if (retryTemplate != null) {
            Assert.state(getErrorChannel() == null,
                    "Cannot have an 'errorChannel' property when 'RetryTemplate' is provided; use an 'ErrorMessageSendingRecoverer' in the 'recoveryCallback' property to send an error message when retries are exhausted");
        }
        Listener messageListener = new Listener();
        SimpleJmsListenerEndpoint jmsListenerEndpoint = new SimpleJmsListenerEndpoint();
        jmsListenerEndpoint.setMessageListener(messageListener);
        messageListenerContainer = listenerContainerFactory
                .createListenerContainer(jmsListenerEndpoint);
        messageListenerContainer.setDestination(jmsConsumerDestination.getDestination());
        messageListenerContainer.setSubscriptionName(group);
        messageListenerContainer.afterPropertiesSet();
        super.onInit();
    }

    @Override
    protected void doStart() {
        messageListenerContainer.start();
    }

    @Override
    protected void doStop() {
        messageListenerContainer.stop();
    }

    @Override
    public int beforeShutdown() {
        stop();
        return 0;
    }

    @Override
    public int afterShutdown() {
        return 0;
    }

    private void setAttributesIfNecessary(Object jmsMessage, org.springframework.messaging.Message<?> message) {
        boolean needHolder = getErrorChannel() != null && retryTemplate == null;
        boolean needAttributes = needHolder || retryTemplate != null;
        if (needHolder) {
            ATTRIBUTES_HOLDER.set(ErrorMessageUtils.getAttributeAccessor(null, null));
        }
        if (needAttributes) {
            AttributeAccessor attributes = retryTemplate != null
                    ? RetrySynchronizationManager.getContext()
                    : ATTRIBUTES_HOLDER.get();
            if (attributes != null) {
                attributes.setAttribute(ErrorMessageUtils.INPUT_MESSAGE_CONTEXT_KEY, message);
                attributes.setAttribute(JmsHeaders.PREFIX + "raw_message", jmsMessage);
            }
        }
    }

    @Override
    protected AttributeAccessor getErrorMessageAttributes(org.springframework.messaging.Message<?> message) {
        AttributeAccessor attributes = ATTRIBUTES_HOLDER.get();
        if (attributes == null) {
            return super.getErrorMessageAttributes(message);
        } else {
            return attributes;
        }
    }

    private Map<String, Object> extractHeaders(Message message) {
        MessageHeaders headers = headerMapper.toHeaders(message);
        SimpMessageHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(headers, SimpMessageHeaderAccessor.class);
        if (Objects.isNull(accessor)) {
            return headers;
        }
        if (bindSourceMessage) {
            accessor.setHeader(IntegrationMessageHeaderAccessor.SOURCE_DATA, message);
        }
        if (retryTemplate != null) {
            accessor.setHeader(IntegrationMessageHeaderAccessor.DELIVERY_ATTEMPT, new AtomicInteger());
        }
        return accessor.getMessageHeaders();
    }

    private Object extractPayload(Message message) {
        try {
            Object payload = messageConverter.fromMessage(message);
            logger.trace(String.format("converted JMS Message [%s] tointegration Message payload [%s]",
                    message, payload));
            return payload;
        } catch (JMSException e) {
            logger.error(e, String.format("Failed to convert message: [%s]", message));
            throw JmsUtils.convertJmsAccessException(e);
        }
    }

    private class Listener implements MessageListener {

        @Override
        public void onMessage(Message message) {
            try {
                org.springframework.messaging.Message<Object> toSend = getMessageBuilderFactory()
                        .withPayload(extractPayload(message))
                        .copyHeaders(extractHeaders(message))
                        .build();

                if (retryTemplate == null) {
                    setAttributesIfNecessary(message, toSend);
                    sendMessage(toSend);
                } else {
                    retryTemplate.execute(context -> {
                        AtomicInteger deliveryAttempt = StaticMessageHeaderAccessor.getDeliveryAttempt(toSend);
                        if (Objects.nonNull(deliveryAttempt)) {
                            deliveryAttempt.incrementAndGet();
                        }

                        setAttributesIfNecessary(message, toSend);
                        sendMessage(toSend);
                        return null;
                    }, recoveryCallback);
                }
            } finally {
                if (retryTemplate == null) {
                    ATTRIBUTES_HOLDER.remove();
                }
            }
        }
    }
}
