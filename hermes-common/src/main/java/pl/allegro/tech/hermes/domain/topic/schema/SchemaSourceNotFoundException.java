package pl.allegro.tech.hermes.domain.topic.schema;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.exception.HermesException;

public class SchemaSourceNotFoundException extends HermesException {

    public SchemaSourceNotFoundException(Topic topic) {
        super("No schema source for topic " + topic.getQualifiedName());
    }

    public SchemaSourceNotFoundException(Topic topic, int schemaVersion) {
        super("No schema source for topic " + topic.getQualifiedName() + " of version " + schemaVersion);
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.OTHER;
    }
}
