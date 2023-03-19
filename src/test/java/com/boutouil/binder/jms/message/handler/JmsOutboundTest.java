package com.boutouil.binder.jms.message.handler;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.IntegrationPatternType;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.SimpleJmsHeaderMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import javax.jms.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class JmsOutboundTest {

    @SneakyThrows
    @Test
    void testSend() {
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        Connection connection = mock(Connection.class);
        when(connectionFactory.createConnection()).thenReturn(connection);
        Session session = mock(Session.class);
        when(connection.createSession(false, Session.AUTO_ACKNOWLEDGE)).thenReturn(session);
        TextMessage textMessage = mock(TextMessage.class);
        when(session.createTextMessage(anyString())).thenReturn(textMessage);
        MessageProducer producer = mock(MessageProducer.class);
        when(session.createProducer(any())).thenReturn(producer);
        MessageConsumer consumer = mock(MessageConsumer.class);
        when(session.createConsumer(any())).thenReturn(consumer);

        JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
        JmsOutbound jmsOutbound = new JmsOutbound(jmsTemplate,
                new SimpleJmsHeaderMapper(), integer -> mock(Destination.class));
        jmsOutbound.setBeanFactory(mock(BeanFactory.class));
        jmsOutbound.doInit();

        jmsOutbound.setExpectReply(false);
        assertThat(jmsOutbound.getComponentType())
                .isEqualTo("jms:outbound-channel-adapter");

        assertThat(jmsOutbound.getIntegrationPatternType())
                .isEqualTo(IntegrationPatternType.outbound_channel_adapter);

        Message<?> message = MessageBuilder.withPayload("TEST").build();
        jmsOutbound.handleRequestMessage(message);

        verify(producer)
                .send(eq(textMessage));
    }

    @SneakyThrows
    @Test
    void testSendAndReceiveNoResponse() {
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        Connection connection = mock(Connection.class);
        when(connectionFactory.createConnection()).thenReturn(connection);
        Session session = mock(Session.class);
        when(connection.createSession(false, Session.AUTO_ACKNOWLEDGE)).thenReturn(session);
        TextMessage textMessage = mock(TextMessage.class);
        when(session.createTextMessage(anyString())).thenReturn(textMessage);
        MessageProducer producer = mock(MessageProducer.class);
        when(session.createProducer(any())).thenReturn(producer);
        MessageConsumer consumer = mock(MessageConsumer.class);
        when(session.createConsumer(any())).thenReturn(consumer);

        JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
        JmsOutbound jmsOutbound = new JmsOutbound(jmsTemplate,
                new SimpleJmsHeaderMapper(), integer -> mock(Destination.class));
        jmsOutbound.setBeanFactory(mock(BeanFactory.class));
        jmsOutbound.doInit();

        jmsOutbound.setExpectReply(true);
        assertThat(jmsOutbound.getComponentType())
                .isEqualTo("jms:outbound-gateway");

        assertThat(jmsOutbound.getIntegrationPatternType())
                .isEqualTo(IntegrationPatternType.outbound_gateway);

        Message<?> message = MessageBuilder.withPayload("TEST").build();
        jmsOutbound.handleRequestMessage(message);

        verify(producer)
                .send(eq(textMessage));
        verify(consumer)
                .receive();
    }
}
