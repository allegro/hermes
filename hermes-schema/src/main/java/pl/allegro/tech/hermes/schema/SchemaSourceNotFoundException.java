package pl.allegro.tech.hermes.schema;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.Topic;

class SchemaSourceNotFoundException extends SchemaException {

    SchemaSourceNotFoundException(Topic topic, SchemaVersion schemaVersion) {
        super("No schema source for topic " + topic.getQualifiedName() + " of version " + schemaVersion.value());
    }

    SchemaSourceNotFoundException(Topic topic) {
        super("No schema source for topic " + topic.getQualifiedName());
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.OTHER;
    }
}
