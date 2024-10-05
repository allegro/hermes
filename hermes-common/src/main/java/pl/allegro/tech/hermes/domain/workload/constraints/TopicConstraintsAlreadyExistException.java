package pl.allegro.tech.hermes.domain.workload.constraints;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.exception.HermesException;

public class TopicConstraintsAlreadyExistException extends HermesException {
  public TopicConstraintsAlreadyExistException(TopicName topicName, Throwable cause) {
    super(
        String.format("Constraints for topic %s already exist.", topicName.qualifiedName()), cause);
  }

  @Override
  public ErrorCode getCode() {
    return ErrorCode.TOPIC_CONSTRAINTS_ALREADY_EXIST;
  }
}
