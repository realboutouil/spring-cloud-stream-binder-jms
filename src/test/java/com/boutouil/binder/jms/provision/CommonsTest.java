package com.boutouil.binder.jms.provision;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.stream.provisioning.ProvisioningException;

import javax.jms.Destination;
import javax.jms.Queue;
import javax.jms.Topic;

import static com.boutouil.binder.jms.provision.Commons.destinationName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommonsTest {

    @SneakyThrows
    @Test
    void testTopic() {
        Topic topic = mock(Topic.class);
        when(topic.getTopicName()).thenReturn("topic");
        String name = destinationName(topic);
        assertThat(name).isEqualTo("topic");
    }

    @SneakyThrows
    @Test
    void testQueue() {
        Queue queue = mock(Queue.class);
        when(queue.getQueueName()).thenReturn("queue");
        String name = destinationName(queue);
        assertThat(name).isEqualTo("queue");
    }

    @Test
    void testDestination() {
        assertThatExceptionOfType(ProvisioningException.class)
                .isThrownBy(() -> destinationName(mock(Destination.class)));
    }

}
