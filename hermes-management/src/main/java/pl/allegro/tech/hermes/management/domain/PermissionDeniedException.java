package pl.allegro.tech.hermes.management.domain;

import pl.allegro.tech.hermes.api.ErrorCode;

public class PermissionDeniedException extends ManagementException {
  public PermissionDeniedException(String message) {
    super(message);
  }

  @Override
  public ErrorCode getCode() {
    return ErrorCode.PERMISSION_DENIED;
  }
}
