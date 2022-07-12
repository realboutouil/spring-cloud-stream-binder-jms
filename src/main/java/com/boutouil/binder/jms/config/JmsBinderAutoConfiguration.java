package com.boutouil.binder.jms.config;

import com.boutouil.binder.jms.JmsMessageChannelBinder;
import com.boutouil.binder.jms.message.handler.JmsMessageHandlerFactory;
import com.boutouil.binder.jms.message.handler.JmsOutbound;
import com.boutouil.binder.jms.properties.JmsConsumerProperties;
import com.boutouil.binder.jms.properties.JmsExtendedBindingProperties;
import com.boutouil.binder.jms.properties.JmsProducerProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.config.ConsumerEndpointCustomizer;
import org.springframework.cloud.stream.config.ProducerMessageHandlerCustomizer;
import org.springframework.cloud.stream.provisioning.ProvisioningProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.JmsHeaderMapper;
import org.springframework.jms.support.SimpleJmsHeaderMapper;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.lang.Nullable;

import javax.jms.ConnectionFactory;
import java.util.Optional;

@Configuration
@ConditionalOnMissingBean(Binder.class)
@ConditionalOnBean(ConnectionFactory.class)
@EnableConfigurationProperties(JmsExtendedBindingProperties.class)
public class JmsBinderAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(JmsTemplate.class)
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
        return new JmsTemplate(connectionFactory);
    }

    @Bean
    @ConditionalOnMissingBean(JmsMessageHandlerFactory.class)
    public JmsMessageHandlerFactory jmsMessageHandlerFactory(
            JmsTemplate jmsTemplate,
            @Nullable JmsHeaderMapper headerMapper,
            @Nullable MessageConverter messageConverter
    ) {
        return new JmsMessageHandlerFactory(jmsTemplate,
                Optional.ofNullable(headerMapper).orElse(new SimpleJmsHeaderMapper()),
                Optional.ofNullable(messageConverter).orElse(new SimpleMessageConverter())
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public JmsMessageChannelBinder jmsMessageChannelBinder(
            JmsMessageHandlerFactory jmsMessageHandlerFactory,
            DefaultJmsListenerContainerFactory jmsListenerContainerFactory,
            ProvisioningProvider<ExtendedConsumerProperties<JmsConsumerProperties>, ExtendedProducerProperties<JmsProducerProperties>> jmsProvisioner,
            JmsExtendedBindingProperties jmsExtendedBindingProperties,
            @Nullable ProducerMessageHandlerCustomizer<JmsOutbound> producerMessageHandlerCustomizer,
            @Nullable ConsumerEndpointCustomizer<MessageProducerSupport> consumerCustomizer
    ) {
        JmsMessageChannelBinder binder = new JmsMessageChannelBinder(
                jmsProvisioner, jmsMessageHandlerFactory, jmsListenerContainerFactory, null
        );
        binder.setExtendedBindingProperties(jmsExtendedBindingProperties);
        binder.setProducerMessageHandlerCustomizer(producerMessageHandlerCustomizer);
        binder.setConsumerEndpointCustomizer(consumerCustomizer);
        return binder;
    }
}
