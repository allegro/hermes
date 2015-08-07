package pl.allegro.tech.hermes.consumers.consumer.receiver;

import pl.allegro.tech.hermes.common.exception.InternalProcessingException;

public class MessageReceivingTimeoutException extends InternalProcessingException {
    public MessageReceivingTimeoutException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public MessageReceivingTimeoutException(String message) {
        super(message);
    }
}
