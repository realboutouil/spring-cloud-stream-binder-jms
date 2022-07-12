package com.boutouil.binder.jms.properties;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

@Data
public class JmsConsumerProperties {

    public static final String DEFAULT_DLQ_POSTFIX = ".dlq";

    /**
     * The dead letter queue naming convention (default is queue-name.dlq).
     */
    @Getter(AccessLevel.NONE)
    private String dlq = DEFAULT_DLQ_POSTFIX;
    /**
     * Specify the internval between reconnection attempts, in milis.
     * Default: 5000ms.
     */
    private long recoveryInterval = 5000;
    /**
     * @see org.springframework.jms.listener.AbstractMessageListenerContainer#setSubscriptionDurable(boolean)
     */
    private boolean durable = true;
    /**
     * @see org.springframework.jms.listener.AbstractMessageListenerContainer#setSubscriptionShared(boolean)
     */
    private boolean shared = true;

    @Nullable
    public String getDlqName(String queueName) {
        if (StringUtils.hasText(dlq)) {
            if (dlq.startsWith(".")) {
                return queueName + dlq;
            } else if (dlq.endsWith(".")) {
                return dlq + queueName;
            } else {
                return dlq;
            }
        } else {
            return null;
        }
    }
}
