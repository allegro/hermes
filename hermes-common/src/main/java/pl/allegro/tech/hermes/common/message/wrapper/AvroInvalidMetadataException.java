package pl.allegro.tech.hermes.common.message.wrapper;

import pl.allegro.tech.hermes.common.exception.InternalProcessingException;

public class AvroInvalidMetadataException extends InternalProcessingException {
  public AvroInvalidMetadataException(String message, Exception cause) {
    super(message, cause);
  }
}
