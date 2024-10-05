package pl.allegro.tech.hermes.management.domain.topic.validator;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.management.domain.ManagementException;

public class TopicValidationException extends ManagementException {

  public TopicValidationException(String message) {
    super(message);
  }

  public TopicValidationException(String message, Exception cause) {
    super(message, cause);
  }

  @Override
  public ErrorCode getCode() {
    return ErrorCode.VALIDATION_ERROR;
  }
}
