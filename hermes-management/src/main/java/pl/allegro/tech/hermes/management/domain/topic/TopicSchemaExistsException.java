package pl.allegro.tech.hermes.management.domain.topic;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.management.domain.ManagementException;

public class TopicSchemaExistsException extends ManagementException {

    TopicSchemaExistsException(String topic) {
        super("Schema already exists for topic " + topic + ", please remove it before creating topic.");
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.SCHEMA_ALREADY_EXISTS;
    }
}
