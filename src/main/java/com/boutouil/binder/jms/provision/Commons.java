package com.boutouil.binder.jms.provision;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.cloud.stream.provisioning.ProvisioningException;
import org.springframework.jms.support.JmsUtils;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Topic;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Commons {

    public static String destinationName(Destination destination) {
        try {
            if (destination instanceof Topic) {
                return ((Topic) destination).getTopicName();
            } else if (destination instanceof Queue) {
                return ((Queue) destination).getQueueName();
            } else {
                throw new JMSException("Not a valid destination: " + destination);
            }
        } catch (JMSException e) {
            throw new ProvisioningException("Error getting destination name",
                    JmsUtils.convertJmsAccessException(e));
        }
    }
}
