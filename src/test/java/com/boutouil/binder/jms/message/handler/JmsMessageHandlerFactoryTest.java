package com.boutouil.binder.jms.message.handler;

import com.boutouil.binder.jms.provision.JmsConsumerDestination;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import javax.jms.ConnectionFactory;
import javax.jms.Queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@Slf4j
@SpringBootTest
@ContextConfiguration(classes = TestContext.class)
@ActiveProfiles("test")
public class JmsMessageHandlerFactoryTest {
    @Autowired
    JmsMessageHandlerFactory jmsSendingMessageHandlerFactory;

    @Test
    void testJmsInboundChannelAdapter() {
        Queue queue = mock(Queue.class);
        Queue dlq = mock(Queue.class);
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);

        DefaultJmsListenerContainerFactory jmsListenerContainerFactory = new DefaultJmsListenerContainerFactory();
        jmsListenerContainerFactory.setConnectionFactory(connectionFactory);

        JmsConsumerDestination jmsConsumerDestination = new JmsConsumerDestination(queue, dlq);
        JmsInboundChannelAdapter adapter = jmsSendingMessageHandlerFactory
                .jmsInboundChannelAdapter(jmsListenerContainerFactory, jmsConsumerDestination, "my-group");

        assertThat(adapter.getComponentType())
                .isEqualTo("jms:inbound-channel-adapter");

        adapter.setOutputChannelName("output-channel-name");
        adapter.onInit();

        assertThat(adapter.beforeShutdown()).isEqualTo(0);
        assertThat(adapter.afterShutdown()).isEqualTo(0);
    }
}
