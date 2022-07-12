package com.boutouil.binder.jms.message.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.SimpleJmsHeaderMapper;
import org.springframework.jms.support.converter.SimpleMessageConverter;

import static org.mockito.Mockito.mock;

@Slf4j
@Configuration
public class TestContext {

    @Bean
    public JmsTemplate jmsTemplate() {
        return mock(JmsTemplate.class);
    }

    @Bean
    public JmsMessageHandlerFactory jmsSendingMessageHandlerFactory(JmsTemplate jmsTemplate) {
        return new JmsMessageHandlerFactory(jmsTemplate,
                new SimpleJmsHeaderMapper(), new SimpleMessageConverter());
    }
}
