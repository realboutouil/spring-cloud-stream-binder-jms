package com.boutouil.binder.jms.message.handler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.jms.JmsException;
import org.springframework.jms.listener.adapter.ListenerExecutionFailedException;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;

public class RejectAndDontRequeueRecoverer implements MessageHandler {
    private final Log logger = LogFactory.getLog(this.getClass());

    @Override
    public void handleMessage(Message<?> message) throws MessagingException {
        Message<?> sourceMessage = StaticMessageHeaderAccessor.getSourceData(message);
        /*
         * NOTE: The following IF and subsequent ELSE IF should never happen
         * under normal interaction and it should always go to the last ELSE
         * However, given that this is a handler subscribing to the public
         * channel and that we can't control what type of Message may be sent
         * to that channel (user decides to send a Message manually) the
         * 'IF/ELSE IF' provides a safety net to handle any message properly.
         */
        if (!(message instanceof ErrorMessage)) {
            logger.error("Expected an ErrorMessage, not a "
                    + message.getClass().toString() + " for: " + message);
            throw new JmsRejectAndDontRequeueException(
                    "Unexpected error message " + message,
                    new JmsRejectAndDontRequeueException(""));
        } else if (sourceMessage == null) {
            logger.error("No raw message header in " + message);
            throw new JmsRejectAndDontRequeueException(
                    "Unexpected error message " + message,
                    new JmsRejectAndDontRequeueException(""));
        } else {
            Throwable cause = (Throwable) message.getPayload();
            if (this.logger.isWarnEnabled()) {
                this.logger.warn("Retries exhausted for message " + message, cause);
            }
            throw new ListenerExecutionFailedException("Retry Policy Exhausted",
                    new JmsRejectAndDontRequeueException(cause));
        }
    }

    public static class JmsRejectAndDontRequeueException extends JmsException {
        private final boolean rejectManual;

        public JmsRejectAndDontRequeueException(String message) {
            this(message, false, null);
        }

        public JmsRejectAndDontRequeueException(Throwable cause) {
            this(null, false, cause);
        }

        public JmsRejectAndDontRequeueException(String message, Throwable cause) {
            this(message, false, cause);
        }

        public JmsRejectAndDontRequeueException(@Nullable String message, boolean rejectManual, @Nullable Throwable cause) {
            super(message, cause);
            this.rejectManual = rejectManual;
        }

        public boolean isRejectManual() {
            return this.rejectManual;
        }
    }
}
