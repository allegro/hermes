package pl.allegro.tech.hermes.domain.group;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.common.exception.HermesException;

public class GroupAlreadyExistsException extends HermesException {

  public GroupAlreadyExistsException(String groupName) {
    super(String.format("Group %s already exists", groupName));
  }

  public GroupAlreadyExistsException(String groupName, Throwable cause) {
    super(String.format("Group %s already exists", groupName), cause);
  }

  @Override
  public ErrorCode getCode() {
    return ErrorCode.GROUP_ALREADY_EXISTS;
  }
}
