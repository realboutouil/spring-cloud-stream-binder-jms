package com.boutouil.binder.jms;

import com.boutouil.binder.jms.properties.JmsProducerProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.cloud.stream.binder.BinderHeaders;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PartitioningInterceptorTest {

    @Test
    void testResolvePartition() {
        PartitioningInterceptor partitioningInterceptor = createPartitioningInterceptor();

        Message<Integer> in = MessageBuilder.withPayload(5).build();
        Message<?> out = partitioningInterceptor.preSend(in, mock(MessageChannel.class));

        assertThat(out.getHeaders())
                .containsEntry(BinderHeaders.PARTITION_HEADER, 5);
    }

    @Test
    void testResolvePartitionOverride() {
        PartitioningInterceptor partitioningInterceptor = createPartitioningInterceptor();

        Message<Integer> in = MessageBuilder.withPayload(5)
                .setHeader(BinderHeaders.PARTITION_OVERRIDE, 3)
                .build();
        Message<?> out = partitioningInterceptor.preSend(in, mock(MessageChannel.class));

        assertThat(out.getHeaders())
                .containsEntry(BinderHeaders.PARTITION_HEADER, 3);
    }

    private PartitioningInterceptor createPartitioningInterceptor() {
        JmsProducerProperties properties = new JmsProducerProperties();
        ExtendedProducerProperties<JmsProducerProperties> pp = new ExtendedProducerProperties<>(properties);
        pp.setPartitionCount(10);
        pp.setPartitionKeyExpression(new SpelExpressionParser().parseExpression("payload"));
        return new PartitioningInterceptor(pp, mock(ConfigurableListableBeanFactory.class));
    }
}
