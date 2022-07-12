package com.boutouil.binder.jms.message.handler;

import com.boutouil.binder.jms.message.handler.expressions.DelayAware;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.cloud.stream.binder.BinderHeaders;
import org.springframework.integration.IntegrationPatternType;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.JmsHeaderMapper;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import javax.jms.Destination;
import java.util.Objects;
import java.util.function.Function;

@RequiredArgsConstructor
public class JmsOutbound extends AbstractReplyProducingMessageHandler {

    private final JmsTemplate jmsTemplate;
    private final JmsHeaderMapper jmsHeaderMapper;
    @Getter
    private final DelayAware delayAware = new DelayAware();
    private final Function<Integer, Destination> exchangeName;
    private boolean expectReply;

    public void setExpectReply(boolean expectReply) {
        this.expectReply = expectReply;
    }

    @Override
    public String getComponentType() {
        return expectReply ? "jms:outbound-gateway" : "jms:outbound-channel-adapter";
    }

    @Override
    protected void doInit() {
        BeanFactory beanFactory = getBeanFactory();
        delayAware.doInit(beanFactory);
    }

    @Override
    public IntegrationPatternType getIntegrationPatternType() {
        return expectReply ? super.getIntegrationPatternType() :
                IntegrationPatternType.outbound_channel_adapter;
    }

    @Override
    protected Object handleRequestMessage(Message<?> message) {
        Integer partition = (Integer) message.getHeaders().get(BinderHeaders.PARTITION_HEADER);
        Destination destination = exchangeName.apply(partition);
        if (expectReply) {
            return sendAndReceive(destination, message);
        } else {
            send(destination, message);
            return null;
        }
    }

    private void send(Destination destination, Message<?> message) {
        Object objectToSend = message.getPayload();
        jmsTemplate.convertAndSend(destination, objectToSend, jmsMessage -> {
            delayAware.addDelayProperty(message, jmsMessage);
            jmsHeaderMapper.fromHeaders(message.getHeaders(), jmsMessage);
            logger.debug(String.format("Sending to destination [%s] JMS message: [%s]",
                    destination, jmsMessage));
            return jmsMessage;
        });
    }

    @SneakyThrows
    private AbstractIntegrationMessageBuilder<?> sendAndReceive(Destination destination, Message<?> message) {
        Object objectToSend = message.getPayload();
        MessageConverter messageConverter = Objects.requireNonNull(jmsTemplate.getMessageConverter());
        javax.jms.Message receivedMessage = jmsTemplate.sendAndReceive(destination, session -> {
            javax.jms.Message jmsMessage = messageConverter.toMessage(objectToSend, session);
            jmsHeaderMapper.fromHeaders(message.getHeaders(), jmsMessage);
            delayAware.addDelayProperty(message, jmsMessage);
            logger.debug(String.format("Sending and waiting to destination [%s] JMS message: [%s]",
                    destination, jmsMessage));
            return jmsMessage;
        });

        if (receivedMessage == null) {
            return null;
        }
        logger.debug(String.format("Received JMS message: [%s]", receivedMessage));
        Object replyObject = messageConverter.fromMessage(receivedMessage);
        MessageHeaders headers = jmsHeaderMapper.toHeaders(receivedMessage);
        AbstractIntegrationMessageBuilder<?> builder = prepareMessageBuilder(replyObject);
        builder.copyHeaders(headers);
        return builder;
    }

    private AbstractIntegrationMessageBuilder<?> prepareMessageBuilder(Object replyObject) {
        return replyObject instanceof Message
                ? getMessageBuilderFactory().fromMessage((Message<?>) replyObject)
                : getMessageBuilderFactory().withPayload(replyObject);
    }
}
