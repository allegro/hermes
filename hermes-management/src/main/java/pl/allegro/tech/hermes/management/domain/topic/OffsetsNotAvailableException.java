package pl.allegro.tech.hermes.management.domain.topic;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.management.domain.ManagementException;

public class OffsetsNotAvailableException extends ManagementException {

  public OffsetsNotAvailableException(Topic topic) {
    super(
        "Not all offsets related to hermes topic " + topic.getQualifiedName() + " were available.");
  }

  @Override
  public ErrorCode getCode() {
    return ErrorCode.OFFSETS_NOT_AVAILABLE_EXCEPTION;
  }
}
