package pl.allegro.tech.hermes.domain.topic.schema;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.exception.HermesException;

public class UnknownSchemaVersionException extends HermesException {

    public UnknownSchemaVersionException(Topic topic, SchemaVersion version) {
        super("Unknown schema version " + version.value() + " for topic " + topic.getQualifiedName());
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.SCHEMA_COULD_NOT_BE_LOADED;
    }
}
