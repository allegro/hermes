package pl.allegro.tech.hermes.management.domain.topic;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.management.domain.ManagementException;

public class UnableToMoveOffsetsException extends ManagementException {

  public UnableToMoveOffsetsException(Topic topic, String subscriptionName) {
    super(
        "Not all offsets related to hermes subscription"
            + topic.getQualifiedName()
            + "$"
            + subscriptionName
            + " were moved.");
  }

  @Override
  public ErrorCode getCode() {
    return ErrorCode.UNABLE_TO_MOVE_OFFSETS_EXCEPTION;
  }
}
