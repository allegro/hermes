package pl.allegro.tech.hermes.common.message.wrapper;

import pl.allegro.tech.hermes.common.exception.InternalProcessingException;

public class WrappingException extends InternalProcessingException {
  public WrappingException(String message, Exception cause) {
    super(message, cause);
  }
}
