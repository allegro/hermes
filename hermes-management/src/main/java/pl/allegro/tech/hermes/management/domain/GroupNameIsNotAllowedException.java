package pl.allegro.tech.hermes.management.domain;

import pl.allegro.tech.hermes.api.ErrorCode;

public class GroupNameIsNotAllowedException extends ManagementException {
  public GroupNameIsNotAllowedException(String message) {
    super(message);
  }

  @Override
  public ErrorCode getCode() {
    return ErrorCode.GROUP_NAME_IS_INVALID;
  }
}
