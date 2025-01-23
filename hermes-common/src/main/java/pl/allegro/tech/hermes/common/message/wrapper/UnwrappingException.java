package pl.allegro.tech.hermes.common.message.wrapper;

import pl.allegro.tech.hermes.common.exception.InternalProcessingException;

public class UnwrappingException extends InternalProcessingException {
  public UnwrappingException(String message, Exception cause) {
    super(message, cause);
  }
}
