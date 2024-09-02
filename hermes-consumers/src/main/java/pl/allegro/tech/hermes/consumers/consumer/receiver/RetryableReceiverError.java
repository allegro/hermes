package pl.allegro.tech.hermes.consumers.consumer.receiver;

import pl.allegro.tech.hermes.common.exception.InternalProcessingException;

public class RetryableReceiverError extends InternalProcessingException {
  public RetryableReceiverError(String message, Throwable throwable) {
    super(message, throwable);
  }
}
