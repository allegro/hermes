package pl.allegro.tech.hermes.management.domain.topic.schema;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.common.exception.HermesException;

public class SchemaRemovalDisabledException extends HermesException {

    public SchemaRemovalDisabledException() {
        super("Removing schema is disabled.");
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.OPERATION_DISABLED;
    }
}
