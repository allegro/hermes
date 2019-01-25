package pl.allegro.tech.hermes.management.domain.topic;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.management.domain.ManagementException;

public class AssignmentsToSubscriptionsNotCompletedException extends ManagementException {

    public AssignmentsToSubscriptionsNotCompletedException(Topic topic) {
        super("Not all subscriptions for topic " + topic.getQualifiedName() + "have assigned consumers");
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.UNABLE_TO_MOVE_OFFSETS_EXCEPTION;
    }
}
