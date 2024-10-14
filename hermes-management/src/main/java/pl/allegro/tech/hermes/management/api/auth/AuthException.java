package pl.allegro.tech.hermes.management.api.auth;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.management.domain.ManagementException;

public class AuthException extends ManagementException {

  public AuthException(String message) {
    super(message);
  }

  public AuthException(RuntimeException ex) {
    super("Exception while authorization: " + ex.getMessage(), ex);
  }

  @Override
  public ErrorCode getCode() {
    return ErrorCode.AUTH_ERROR;
  }
}
