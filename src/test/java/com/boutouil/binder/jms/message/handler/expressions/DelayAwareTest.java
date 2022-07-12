package com.boutouil.binder.jms.message.handler.expressions;

import com.boutouil.binder.jms.properties.JmsProducerProperties;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import static com.boutouil.binder.jms.message.handler.expressions.DelayAware.X_DELAY;
import static org.mockito.Mockito.*;

class DelayAwareTest {

    @SneakyThrows
    @Test
    void testDelayValue() {
        DelayAware delayAware = new DelayAware();
        delayAware.setDelay(5);
        delayAware.doInit(null);

        Message<String> message = MessageBuilder.withPayload("TEST").build();
        javax.jms.Message jmsMessage = mock(javax.jms.Message.class);
        delayAware.addDelayProperty(message, jmsMessage);
        verify(jmsMessage).setIntProperty(X_DELAY, 5);
    }

    @SneakyThrows
    @Test
    void testNoDelay() {
        JmsProducerProperties jmsProducerProperties = new JmsProducerProperties();
        ExtendedProducerProperties<JmsProducerProperties> producerProperties = new ExtendedProducerProperties<>(jmsProducerProperties);
        jmsProducerProperties.setDelayExpression(new ValueExpression<>(-1));

        DelayAware delayAware = new DelayAware();
        delayAware.init(producerProperties);
        delayAware.doInit(null);

        Message<String> message = MessageBuilder.withPayload("TEST").build();
        javax.jms.Message jmsMessage = mock(javax.jms.Message.class);
        delayAware.addDelayProperty(message, jmsMessage);
        verify(jmsMessage, never()).setIntProperty(eq(X_DELAY), anyInt());
    }

    @SneakyThrows
    @Test
    void testNoDelayDefined() {
        JmsProducerProperties jmsProducerProperties = new JmsProducerProperties();
        ExtendedProducerProperties<JmsProducerProperties> producerProperties = new ExtendedProducerProperties<>(jmsProducerProperties);

        DelayAware delayAware = new DelayAware();
        delayAware.init(producerProperties);
        delayAware.doInit(null);

        Message<String> message = MessageBuilder.withPayload("TEST").build();
        javax.jms.Message jmsMessage = mock(javax.jms.Message.class);
        delayAware.addDelayProperty(message, jmsMessage);
        verify(jmsMessage, never()).setIntProperty(eq(X_DELAY), anyInt());
    }

    @SneakyThrows
    @Test
    void testDelayExpression() {
        JmsProducerProperties jmsProducerProperties = new JmsProducerProperties();
        ExtendedProducerProperties<JmsProducerProperties> producerProperties = new ExtendedProducerProperties<>(jmsProducerProperties);
        jmsProducerProperties.setDelayExpression(
                new SpelExpressionParser().parseExpression("payload")
        );

        DelayAware delayAware = new DelayAware();
        delayAware.init(producerProperties);
        delayAware.doInit(mock(BeanFactory.class));

        Message<String> message = MessageBuilder.withPayload("5").build();
        javax.jms.Message jmsMessage = mock(javax.jms.Message.class);
        delayAware.addDelayProperty(message, jmsMessage);
        verify(jmsMessage).setIntProperty(X_DELAY, 5);
    }
}
