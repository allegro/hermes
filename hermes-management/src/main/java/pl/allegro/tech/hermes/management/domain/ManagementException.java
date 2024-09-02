package pl.allegro.tech.hermes.management.domain;

import pl.allegro.tech.hermes.api.ErrorCode;

public abstract class ManagementException extends RuntimeException {

  public ManagementException(Throwable t) {
    super(t);
  }

  public ManagementException(String message) {
    super(message);
  }

  public ManagementException(String message, Throwable cause) {
    super(message, cause);
  }

  public abstract ErrorCode getCode();
}
