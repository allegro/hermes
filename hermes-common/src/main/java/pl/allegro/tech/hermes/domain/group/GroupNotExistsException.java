package pl.allegro.tech.hermes.domain.group;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.common.exception.HermesException;

public class GroupNotExistsException extends HermesException {

  public GroupNotExistsException(String groupName, Exception exception) {
    super(String.format("Group %s does not exist", groupName), exception);
  }

  public GroupNotExistsException(String groupName) {
    this(groupName, null);
  }

  @Override
  public ErrorCode getCode() {
    return ErrorCode.GROUP_NOT_EXISTS;
  }
}
