package com.boutouil.binder.jms.properties;

import lombok.Data;
import org.springframework.expression.Expression;
import org.springframework.lang.Nullable;

@Data
public class JmsProducerProperties {

    /**
     * SPeL expression to determine the delay to apply when sending jms messages.
     */
    @Nullable
    private Expression delayExpression;
}
