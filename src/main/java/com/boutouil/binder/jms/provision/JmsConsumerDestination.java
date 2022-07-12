package com.boutouil.binder.jms.provision;

import lombok.Data;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.lang.Nullable;

import javax.jms.Destination;
import javax.jms.Topic;

import static com.boutouil.binder.jms.provision.Commons.destinationName;

@Data
public class JmsConsumerDestination implements ConsumerDestination {

    private final Destination destination;
    @Nullable
    private final Destination dlq;

    public boolean isPubSub() {
        return destination instanceof Topic;
    }

    @Override
    public String getName() {
        return destinationName(destination);
    }

}
