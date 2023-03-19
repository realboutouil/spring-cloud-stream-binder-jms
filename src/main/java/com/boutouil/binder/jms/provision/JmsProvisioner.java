package com.boutouil.binder.jms.provision;

import com.boutouil.binder.jms.properties.JmsConsumerProperties;
import com.boutouil.binder.jms.properties.JmsProducerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.cloud.stream.provisioning.ConsumerDestination;
import org.springframework.cloud.stream.provisioning.ProducerDestination;
import org.springframework.cloud.stream.provisioning.ProvisioningException;
import org.springframework.cloud.stream.provisioning.ProvisioningProvider;
import org.springframework.jms.support.JmsUtils;

import jakarta.jms.Queue;
import jakarta.jms.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class JmsProvisioner implements ProvisioningProvider<
        ExtendedConsumerProperties<JmsConsumerProperties>,
        ExtendedProducerProperties<JmsProducerProperties>> {

    private final ConnectionFactory connectionFactory;

    @Override
    public ProducerDestination provisionProducerDestination(
            String destinationName,
            ExtendedProducerProperties<JmsProducerProperties> properties) throws ProvisioningException {
        List<String> destinations = new ArrayList<>();
        if (properties.isPartitioned()) {
            Map<Integer, String> idToPartition = new HashMap<>();
            for (int i = 0; i < properties.getPartitionCount(); i++) {
                String destination = buildName(i, destinationName);
                destinations.add(destination);
                idToPartition.put(i, destination);
            }

            Map<String, Destination> destinationss = provision(destinations);
            Map<Integer, Destination> indexToDestination = idToPartition.entrySet()
                    .stream().collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> destinationss.get(e.getValue())
                    ));
            return new JmsProducerDestination(destinationName, indexToDestination);
        } else {
            destinations.add(destinationName);
            Map<String, Destination> destinationss = provision(destinations);
            return new JmsProducerDestination(destinationss.get(destinationName));
        }
    }

    @Override
    public ConsumerDestination provisionConsumerDestination(
            String destinationName,
            String group,
            ExtendedConsumerProperties<JmsConsumerProperties> properties) throws ProvisioningException {
        List<String> destinations = new ArrayList<>();
        destinations.add(destinationName);
        //Dead-letter-queue
        String noPrefix = noPrefix(destinationName);
        String prefix = destinationName.replaceFirst(noPrefix, "");
        String dlq = properties.getExtension().getDlqName(noPrefix);
        String dlqKey = Optional.ofNullable(dlq)
                .map(s -> {
                    String dlqName = prefix + dlq;
                    destinations.add(dlqName);
                    return dlqName;
                })
                .orElse(null);

        Map<String, Destination> provisioned = provision(destinations);
        return new JmsConsumerDestination(provisioned.get(destinationName), provisioned.get(dlqKey));
    }

    /**
     * Build a name for the destination (queue/topic) based on the partition index.
     */
    private String buildName(int partitionIndex, String group) {
        return String.format("%s-%s", group, partitionIndex);
    }

    private Map<String, Destination> provision(List<String> destinationsToProvision) {
        Map<String, Destination> destinations = new HashMap<>();
        if (destinationsToProvision.isEmpty()) {
            return Map.of();
        }

        try (Connection connection = connectionFactory.createConnection()) {
            try (Session session = connection.createSession()) {
                for (String destination : destinationsToProvision) {
                    String noPrefix = noPrefix(destination);
                    if (destination.startsWith("topic://")) {
                        Topic topic = session.createTopic(noPrefix);
                        destinations.put(destination, topic);
                        log.info("Provisioned: {}", topic);
                    } else if (destination.startsWith("queue://")) {
                        Queue queue = session.createQueue(noPrefix);
                        destinations.put(destination, queue);
                        log.info("Provisioned: {}", queue);
                    } else {
                        log.warn("Cannot provision topic/queue name {}. It should start with 'topic://' or 'queue://'.", destination);
                    }
                }
                JmsUtils.commitIfNecessary(session);
            } catch (JMSException ex) {
                throw new ProvisioningException("Could not create session.", ex);
            }
        } catch (JMSException e) {
            throw new ProvisioningException("Could not create connection.", e);
        }
        return destinations;
    }

    protected String noPrefix(String destinationName) {
        return destinationName.substring(8);
    }
}
