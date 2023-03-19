package com.boutouil.binder.jms.message.handler.expressions;

import com.boutouil.binder.jms.properties.JmsProducerProperties;
import lombok.SneakyThrows;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.expression.Expression;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;
import org.springframework.messaging.Message;

public class DelayAware {
    public static final String X_DELAY = "x_delay";

    private Expression delayExpression;
    private ExpressionEvaluatingMessageProcessor<Integer> delayGenerator;

    /**
     * Set the delay in the {@code x-delay} header.
     *
     * @param delay the value of the delay.
     */
    public void setDelay(int delay) {
        delayExpression = new ValueExpression<>(delay);
    }

    public void doInit(BeanFactory beanFactory) {
        if (delayExpression != null) {
            delayGenerator = new ExpressionEvaluatingMessageProcessor<>(delayExpression, Integer.class);
            if (beanFactory != null) {
                delayGenerator.setBeanFactory(beanFactory);
            }
        }
    }

    @SneakyThrows
    public void addDelayProperty(Message<?> message, jakarta.jms.Message jmsMessage) {
        if (delayGenerator != null) {
            Integer delay = delayGenerator.processMessage(message);
            if (delay != null && delay >= 0) {
                jmsMessage.setIntProperty(X_DELAY, delay);
            }
        }
    }

    public void init(ExtendedProducerProperties<JmsProducerProperties> producerProperties) {
        JmsProducerProperties extension = producerProperties.getExtension();
        if (extension.getDelayExpression() != null) {
            setDelayExpression(extension.getDelayExpression());
        }
    }

    /**
     * Set the delay SpEL expression that will evaluate the {@code x-delay} header.
     *
     * @param delayExpression the SpEL expression for the delay.
     */
    public void setDelayExpression(Expression delayExpression) {
        this.delayExpression = delayExpression;
    }
}
