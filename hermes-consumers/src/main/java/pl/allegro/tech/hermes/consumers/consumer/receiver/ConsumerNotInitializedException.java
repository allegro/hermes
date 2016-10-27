package pl.allegro.tech.hermes.consumers.consumer.receiver;

import pl.allegro.tech.hermes.common.exception.InternalProcessingException;

public class ConsumerNotInitializedException extends InternalProcessingException {
    public ConsumerNotInitializedException() {
        super("Please make sure that you call initialize first.");
    }
}
