package pl.allegro.tech.hermes.domain.topic;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.exception.HermesException;

public class TopicNotEmptyException extends HermesException {

  public TopicNotEmptyException(TopicName topicName) {
    super(String.format("Topic %s is not empty", topicName.qualifiedName()));
  }

  @Override
  public ErrorCode getCode() {
    return ErrorCode.TOPIC_NOT_EMPTY;
  }
}
