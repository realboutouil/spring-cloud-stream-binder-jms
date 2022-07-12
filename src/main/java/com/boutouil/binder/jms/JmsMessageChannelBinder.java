package com.boutouil.binder.jms;

import com.boutouil.binder.jms.message.handler.JmsInboundChannelAdapter;
import com.boutouil.binder.jms.message.handler.JmsMessageHandlerFactory;
import com.boutouil.binder.jms.message.handler.RejectAndDontRequeueRecoverer;
import com.boutouil.binder.jms.properties.JmsConsumerProperties;
import com.boutouil.binder.jms.properties.JmsExtendedBindingProperties;
import com.boutouil.binder.jms.properties.JmsProducerProperties;
import com.boutouil.binder.jms.provision.JmsConsumerDestination;
import com.boutouil.binder.jms.provision.JmsProducerDestination;
import org.springframework.cloud.stream.binder.*;
import org.springframework.cloud.stream.config.ListenerContainerCustomizer;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.cloud.stream.provisioning.ProvisioningProvider;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.support.DefaultErrorMessageStrategy;
import org.springframework.integration.support.ErrorMessageStrategy;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import java.util.Objects;

public class JmsMessageChannelBinder extends AbstractMessageChannelBinder<
        ExtendedConsumerProperties<JmsConsumerProperties>,
        ExtendedProducerProperties<JmsProducerProperties>,
        ProvisioningProvider<ExtendedConsumerProperties<JmsConsumerProperties>, ExtendedProducerProperties<JmsProducerProperties>>
        > implements ExtendedPropertiesBinder<MessageChannel, JmsConsumerProperties, JmsProducerProperties> {

    private final JmsMessageHandlerFactory jmsMessageHandlerFactory;
    private final DefaultJmsListenerContainerFactory jmsListenerContainerFactory;
    private JmsExtendedBindingProperties extendedBindingProperties = new JmsExtendedBindingProperties();

    public JmsMessageChannelBinder(
            ProvisioningProvider<ExtendedConsumerProperties<JmsConsumerProperties>,
                    ExtendedProducerProperties<JmsProducerProperties>> provisioningProvider,
            JmsMessageHandlerFactory jmsMessageHandlerFactory,
            DefaultJmsListenerContainerFactory jmsListenerContainerFactory,
            ListenerContainerCustomizer<AbstractMessageListenerContainer> containerCustomizer) {
        super(new String[0], provisioningProvider, containerCustomizer, null);
        this.jmsMessageHandlerFactory = jmsMessageHandlerFactory;
        this.jmsListenerContainerFactory = jmsListenerContainerFactory;
    }

    public void setExtendedBindingProperties(JmsExtendedBindingProperties extendedBindingProperties) {
        this.extendedBindingProperties = extendedBindingProperties;
    }

    @Override
    public JmsConsumerProperties getExtendedConsumerProperties(String channelName) {
        return extendedBindingProperties.getExtendedConsumerProperties(channelName);
    }

    @Override
    public JmsProducerProperties getExtendedProducerProperties(String channelName) {
        return extendedBindingProperties.getExtendedProducerProperties(channelName);
    }

    @Override
    public String getDefaultsPrefix() {
        return extendedBindingProperties.getDefaultsPrefix();
    }

    @Override
    public Class<? extends BinderSpecificPropertiesProvider> getExtendedPropertiesEntryClass() {
        return extendedBindingProperties.getExtendedPropertiesEntryClass();
    }

    @Override
    protected MessageHandler createProducerMessageHandler(
            ProducerDestination destination,
            ExtendedProducerProperties<JmsProducerProperties> producerProperties,
            MessageChannel errorChannel) {
        return jmsMessageHandlerFactory
                .jmsOutbound((JmsProducerDestination) destination, producerProperties);
    }

    @Override
    protected void postProcessOutputChannel(
            MessageChannel outputChannel,
            ExtendedProducerProperties<JmsProducerProperties> producerProperties) {
        if (producerProperties.isPartitioned()) {
            ((AbstractMessageChannel) outputChannel)
                    .addInterceptor(0,
                            new PartitioningInterceptor(producerProperties, getBeanFactory()));
        }
    }

    @Override
    protected MessageProducer createConsumerEndpoint(
            ConsumerDestination destination,
            String group,
            ExtendedConsumerProperties<JmsConsumerProperties> properties) {
        JmsConsumerProperties jmsConsumerProperties = properties.getExtension();
        jmsListenerContainerFactory.setRecoveryInterval(jmsConsumerProperties.getRecoveryInterval());
        jmsListenerContainerFactory.setTaskExecutor(
                new SimpleAsyncTaskExecutor(destination.getName() + "-")
        );
        getContainerCustomizer().configure(jmsListenerContainerFactory, destination.getName(), group);

        jmsListenerContainerFactory.setPubSubDomain(((JmsConsumerDestination) destination).isPubSub());
        jmsListenerContainerFactory.setSubscriptionDurable(jmsConsumerProperties.isDurable());
        jmsListenerContainerFactory.setSubscriptionShared(jmsConsumerProperties.isShared());

        JmsInboundChannelAdapter adapter = jmsMessageHandlerFactory
                .jmsInboundChannelAdapter(jmsListenerContainerFactory, (JmsConsumerDestination) destination, group);
        adapter.setAutoStartup(properties.isAutoStartup());
        adapter.setBindSourceMessage(true);
        adapter.setBeanName(String.join(".", "inbound", destination.getName(), group));
        ErrorInfrastructure errorInfrastructure = registerErrorInfrastructure(destination, group, properties);
        if (properties.getMaxAttempts() > 1) {
            adapter.setRetryTemplate(buildRetryTemplate(properties));
            adapter.setRecoveryCallback(errorInfrastructure.getRecoverer());
        } else {
            adapter.setErrorMessageStrategy(getErrorMessageStrategy());
            adapter.setErrorChannel(errorInfrastructure.getErrorChannel());
        }
        return adapter;
    }

    @Override
    protected ErrorMessageStrategy getErrorMessageStrategy() {
        return new DefaultErrorMessageStrategy();
    }

    @Override
    protected MessageHandler getErrorMessageHandler(ConsumerDestination destination,
                                                    String group,
                                                    final ExtendedConsumerProperties<JmsConsumerProperties> properties) {
        JmsConsumerDestination jmsConsumerDestination = (JmsConsumerDestination) destination;

        if (Objects.nonNull(jmsConsumerDestination.getDlq())) {
            return jmsMessageHandlerFactory.errorMessageHandler(jmsConsumerDestination);
        } else if (properties.getMaxAttempts() > 1) {
            return new RejectAndDontRequeueRecoverer();
        } else {
            return super.getErrorMessageHandler(destination, group, properties);
        }
    }

//    @Override
//    protected MessageHandler getPolledConsumerErrorMessageHandler(
//            ConsumerDestination destination, String group,
//            ExtendedConsumerProperties<RabbitConsumerProperties> properties) {
//        MessageHandler handler = getErrorMessageHandler(destination, group, properties);
//        if (handler != null) {
//            return handler;
//        }
//        final MessageHandler superHandler = super.getErrorMessageHandler(destination,
//                group, properties);
//        return message -> {
//            Message amqpMessage = (Message) message.getHeaders()
//                    .get(AmqpMessageHeaderErrorMessageStrategy.AMQP_RAW_MESSAGE);
//            if (!(message instanceof ErrorMessage)) {
//                logger.error("Expected an ErrorMessage, not a "
//                        + message.getClass().toString() + " for: " + message);
//            }
//            else if (amqpMessage == null) {
//                if (superHandler != null) {
//                    superHandler.handleMessage(message);
//                }
//            }
//            else {
//                if (message.getPayload() instanceof MessagingException) {
//                    AcknowledgmentCallback ack = StaticMessageHeaderAccessor
//                            .getAcknowledgmentCallback(
//                                    ((MessagingException) message.getPayload())
//                                            .getFailedMessage());
//                    if (ack != null) {
//                        if (properties.getExtension().isRequeueRejected()) {
//                            ack.acknowledge(Status.REQUEUE);
//                        }
//                        else {
//                            ack.acknowledge(Status.REJECT);
//                        }
//                    }
//                }
//            }
//        };
//    }
}
