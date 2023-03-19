package com.boutouil.binder.jms.message.handler;

import com.boutouil.binder.jms.properties.JmsProducerProperties;
import com.boutouil.binder.jms.provision.JmsConsumerDestination;
import com.boutouil.binder.jms.provision.JmsProducerDestination;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jms.config.AbstractJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.support.JmsHeaderMapper;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.messaging.MessageHandler;

import jakarta.jms.Destination;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
public class JmsMessageHandlerFactory implements ApplicationContextAware, BeanFactoryAware {

    private final JmsTemplate jmsTemplate;
    private final JmsHeaderMapper jmsHeaderMapper;
    private final MessageConverter messageConverter;
    private ApplicationContext applicationContext;
    private BeanFactory beanFactory;

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public JmsInboundChannelAdapter jmsInboundChannelAdapter(
            AbstractJmsListenerContainerFactory<DefaultMessageListenerContainer> jmsListenerContainerFactory,
            JmsConsumerDestination jmsConsumerDestination,
            String group
    ) {
        JmsInboundChannelAdapter adapter = new JmsInboundChannelAdapter(jmsListenerContainerFactory, jmsConsumerDestination, group, messageConverter, jmsHeaderMapper);
        adapter.setBindSourceMessage(true);
        adapter.setBeanFactory(beanFactory);
        adapter.setApplicationContext(applicationContext);
        return adapter;
    }

    public DlqErrorSendingMessageHandler errorMessageHandler(JmsConsumerDestination jmsConsumerDestination) {
        Destination deadLetterQueue = jmsConsumerDestination.getDlq();
        if (Objects.isNull(deadLetterQueue)) {
            return null;
        }
        DlqErrorSendingMessageHandler handler = new DlqErrorSendingMessageHandler(deadLetterQueue, jmsTemplate, jmsHeaderMapper);
        handler.setApplicationContext(applicationContext);
        handler.setBeanFactory(beanFactory);
        handler.afterPropertiesSet();
        return handler;
    }

    public MessageHandler jmsOutbound(
            JmsProducerDestination jmsProducerDestination,
            ExtendedProducerProperties<JmsProducerProperties> producerProperties
    ) {
        JmsOutbound outbound = new JmsOutbound(jmsTemplate, jmsHeaderMapper, jmsProducerDestination::getDestination);
        outbound.getDelayAware().init(producerProperties);
        outbound.setBeanFactory(beanFactory);
        return outbound;
    }
}
