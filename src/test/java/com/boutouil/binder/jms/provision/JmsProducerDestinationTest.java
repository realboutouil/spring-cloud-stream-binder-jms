package com.boutouil.binder.jms.provision;

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import jakarta.jms.Queue;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JmsProducerDestinationTest {

    @SneakyThrows
    @Test
    void testSingle() {
        Queue queue = mock(Queue.class);
        when(queue.getQueueName()).thenReturn("queue");
        JmsProducerDestination producerDestination = new JmsProducerDestination(queue);

        assertThat(producerDestination.getDestination(5))
                .isEqualTo(queue);
        assertThat(producerDestination.getDestination(-1))
                .isEqualTo(queue);
        assertThat(producerDestination.getName())
                .isEqualTo("queue");
        assertThat(producerDestination.getNameForPartition(5))
                .isEqualTo("queue");
        assertThat(producerDestination.getNameForPartition(-1))
                .isEqualTo("queue");
    }

    @SneakyThrows
    @Test
    void testPartition() {
        Queue queue1 = mock(Queue.class);
        when(queue1.getQueueName()).thenReturn("queue-1");
        Queue queue2 = mock(Queue.class);
        when(queue2.getQueueName()).thenReturn("queue-2");
        JmsProducerDestination producerDestination = new JmsProducerDestination("queue",
                Map.of(1, queue1, 2, queue2));

        assertThat(producerDestination.getDestination(2))
                .isEqualTo(queue2);
        assertThat(producerDestination.getDestination(1))
                .isEqualTo(queue1);
        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> producerDestination.getDestination(-1));
        assertThat(producerDestination.getName())
                .isEqualTo("queue");
        assertThat(producerDestination.getNameForPartition(2))
                .isEqualTo("queue-2");
        assertThat(producerDestination.getNameForPartition(1))
                .isEqualTo("queue-1");
        assertThat(producerDestination.getNameForPartition(-1))
                .isEqualTo("queue");
    }
}
