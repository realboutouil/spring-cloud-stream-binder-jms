package com.boutouil.binder.jms.message.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.JmsHeaderMapper;
import org.springframework.jms.support.JmsHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.ErrorMessage;

import javax.jms.Destination;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
public class DlqErrorSendingMessageHandler extends AbstractMessageHandler {
    private static final String X_EXCEPTION_MESSAGE = "x_exception_message";
    private static final String X_ORIGINAL_DESTINATION = "x_original_destination";
    private static final String X_EXCEPTION_STACKTRACE = "x_exception_stacktrace";

    private final Destination deadLetterQueue;
    private final JmsTemplate jmsTemplate;
    private final JmsHeaderMapper jmsHeaderMapper;

    @Override
    protected void handleMessageInternal(Message<?> message) {
        if (message instanceof ErrorMessage) {
            Message<?> originalMessage = ((ErrorMessage) message).getOriginalMessage();

            Object payload = originalMessage != null ? originalMessage.getPayload() : null;
            MessageHeaders originalHeaders = originalMessage != null ? originalMessage.getHeaders() : null;
            Throwable cause = (Throwable) message.getPayload();

            jmsTemplate.convertAndSend(deadLetterQueue, payload, jmsMessage -> {
                Map<String, Object> headers = new HashMap<>();
                headers.put(X_EXCEPTION_MESSAGE,
                        cause.getCause() != null ? cause.getCause().getMessage() : cause.getMessage());
                headers.put(X_EXCEPTION_STACKTRACE, getStackTraceAsString(cause));
                Object originalDestination = originalHeaders != null ?
                        originalHeaders.get(JmsHeaders.DESTINATION) : null;
                headers.put(X_ORIGINAL_DESTINATION, Optional.ofNullable(originalDestination)
                        .map(Object::toString).orElse(null));

                jmsHeaderMapper.fromHeaders(new MessageHeaders(headers), jmsMessage);
                logger.debug(String.format("Sending to dead-letter-queue [%s] JMS message: [%s]",
                        deadLetterQueue, jmsMessage));
                return jmsMessage;
            });
        } else {
            logger.error(String.format("Expected an ErrorMessage, got %s for %s",
                    message.getClass().toString(), message));
        }
    }

    private String getStackTraceAsString(Throwable cause) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter, true);
        cause.printStackTrace(printWriter);
        return stringWriter.getBuffer().toString();
    }
}
