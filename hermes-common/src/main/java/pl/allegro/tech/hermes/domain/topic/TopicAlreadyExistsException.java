package pl.allegro.tech.hermes.domain.topic;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.exception.HermesException;

public class TopicAlreadyExistsException extends HermesException {

    public TopicAlreadyExistsException(TopicName topicName, Throwable cause) {
        super(String.format("Topic %s already exists", topicName.qualifiedName()), cause);
    }

    public TopicAlreadyExistsException(TopicName topicName) {
        super(String.format("Topic %s already exists", topicName.qualifiedName()));
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.TOPIC_ALREADY_EXISTS;
    }
}
