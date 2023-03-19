package com.boutouil.binder.jms.provision;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.cloud.stream.provisioning.ProducerDestination;

import jakarta.jms.Destination;
import java.util.Map;
import java.util.Optional;

@EqualsAndHashCode
@ToString
public class JmsProducerDestination implements ProducerDestination {
    private final String destinationName;
    private final Map<Integer, Destination> partitions;

    public JmsProducerDestination(Destination destination) {
        this(Commons.destinationName(destination), Map.of(-1, destination));
    }

    public JmsProducerDestination(String destinationName, Map<Integer, Destination> partitions) {
        this.destinationName = destinationName;
        this.partitions = partitions;
    }

    @Override
    public String getName() {
        return destinationName;
    }

    @Override
    public String getNameForPartition(int partition) {
        return Optional.ofNullable(partitions.get(partition))
                .map(Commons::destinationName)
                .orElse(destinationName);
    }

    public Destination getDestination(Integer partition) {
        return Optional.ofNullable(partition)
                .map(partitions::get)
                .orElseGet(() -> Optional.ofNullable(partitions.get(-1))
                        .orElseThrow());
    }
}
