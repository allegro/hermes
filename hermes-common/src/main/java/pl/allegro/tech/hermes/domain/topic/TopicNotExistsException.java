package pl.allegro.tech.hermes.domain.topic;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.exception.HermesException;

public class TopicNotExistsException extends HermesException {

  public TopicNotExistsException(TopicName topicName, Exception exception) {
    super(String.format("Topic %s does not exist", topicName.qualifiedName()), exception);
  }

  public TopicNotExistsException(TopicName topicName) {
    this(topicName, null);
  }

  @Override
  public ErrorCode getCode() {
    return ErrorCode.TOPIC_NOT_EXISTS;
  }
}
