package pl.allegro.tech.hermes.schema;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.Topic;

class SchemaNotFoundException extends SchemaException {

    SchemaNotFoundException(Topic topic, SchemaVersion schemaVersion) {
        super("No schema source for topic " + topic.getQualifiedName() + " of version " + schemaVersion.value());
    }

    SchemaNotFoundException(Topic topic) {
        super("No schema source for topic " + topic.getQualifiedName());
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.OTHER;
    }
}
