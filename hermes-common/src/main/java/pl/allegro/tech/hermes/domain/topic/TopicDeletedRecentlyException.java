package pl.allegro.tech.hermes.domain.topic;

import java.time.Instant;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.exception.HermesException;


public class TopicDeletedRecentlyException extends HermesException {

public TopicDeletedRecentlyException(TopicName topicName, Instant thresholdTime) {
	super(String.format("Topic %s cannot created until %s", topicName.qualifiedName(), thresholdTime.toString()));
}


@Override
public ErrorCode getCode() { // TODO Auto-generated method stub
	return ErrorCode.TOPIC_CREATED_RECENTLY;
}

}

