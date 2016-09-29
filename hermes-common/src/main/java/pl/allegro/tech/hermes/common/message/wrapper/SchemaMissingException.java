package pl.allegro.tech.hermes.common.message.wrapper;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.exception.HermesException;

public class SchemaMissingException extends HermesException {

    SchemaMissingException(Topic topic) {
        super("Schema for topic " + topic.getQualifiedName() + " was not available");
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.SCHEMA_COULD_NOT_BE_LOADED;
    }

}
