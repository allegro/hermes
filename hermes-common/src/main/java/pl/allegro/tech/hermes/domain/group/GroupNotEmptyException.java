package pl.allegro.tech.hermes.domain.group;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.common.exception.HermesException;

public class GroupNotEmptyException extends HermesException {

  public GroupNotEmptyException(String groupName) {
    super(String.format("Group %s is not empty", groupName));
  }

  @Override
  public ErrorCode getCode() {
    return ErrorCode.GROUP_NOT_EMPTY;
  }
}
