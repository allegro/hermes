package pl.allegro.tech.hermes.management.domain.topic;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.management.domain.ManagementException;

public class TopicRemovalDisabledException extends ManagementException {

    public TopicRemovalDisabledException(TopicName topicName) {
        super(String.format("Could not remove topic %s, this operation is currently disabled.", topicName.qualifiedName()));
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.OPERATION_DISABLED;
    }

}
