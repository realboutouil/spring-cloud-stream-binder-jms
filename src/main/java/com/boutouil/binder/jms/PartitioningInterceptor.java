package com.boutouil.binder.jms;

import com.boutouil.binder.jms.properties.JmsProducerProperties;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.cloud.stream.binder.BinderHeaders;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.binder.PartitionHandler;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.integration.support.MutableMessageBuilderFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;

public class PartitioningInterceptor implements ChannelInterceptor {

    private final PartitionHandler partitionHandler;
    private final MessageBuilderFactory messageBuilderFactory = new MutableMessageBuilderFactory();

    PartitioningInterceptor(
            ExtendedProducerProperties<JmsProducerProperties> producerProperties,
            ConfigurableListableBeanFactory beanFactory
    ) {
        partitionHandler = new PartitionHandler(
                ExpressionUtils.createStandardEvaluationContext(beanFactory),
                producerProperties,
                beanFactory
        );
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        if (!message.getHeaders().containsKey(BinderHeaders.PARTITION_OVERRIDE)) {
            int partition = partitionHandler.determinePartition(message);
            return messageBuilderFactory.fromMessage(message)
                    .setHeader(BinderHeaders.PARTITION_HEADER, partition)
                    .build();
        } else {
            return messageBuilderFactory.fromMessage(message)
                    .setHeader(BinderHeaders.PARTITION_HEADER,
                            message.getHeaders().get(BinderHeaders.PARTITION_OVERRIDE))
                    .build();
        }
    }
}
