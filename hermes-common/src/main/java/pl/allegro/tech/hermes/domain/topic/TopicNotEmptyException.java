package pl.allegro.tech.hermes.domain.topic;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.exception.HermesException;

public class TopicNotEmptyException extends HermesException {

  public TopicNotEmptyException(TopicName topicName) {
    super(
        String.format(
            "Topic %s has subscriptions without autoDeleteWithTopic enabled."
                + " Remove them or enable this flag before deleting the topic.",
            topicName.qualifiedName()));
  }

  @Override
  public ErrorCode getCode() {
    return ErrorCode.TOPIC_NOT_EMPTY;
  }
}
