package pl.allegro.tech.hermes.domain.workload.constraints;

import static pl.allegro.tech.hermes.api.ErrorCode.TOPIC_CONSTRAINTS_DO_NOT_EXIST;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.exception.HermesException;

public class TopicConstraintsDoNotExistException extends HermesException {

  public TopicConstraintsDoNotExistException(TopicName topicName, Throwable cause) {
    super(
        String.format("Constraints for topic %s do not exist.", topicName.qualifiedName()), cause);
  }

  @Override
  public ErrorCode getCode() {
    return TOPIC_CONSTRAINTS_DO_NOT_EXIST;
  }
}
