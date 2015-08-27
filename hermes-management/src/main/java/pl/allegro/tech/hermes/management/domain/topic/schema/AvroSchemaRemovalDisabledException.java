package pl.allegro.tech.hermes.management.domain.topic.schema;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.common.exception.HermesException;

public class AvroSchemaRemovalDisabledException extends HermesException {

    public AvroSchemaRemovalDisabledException(String message) {
        super(message);
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.AVRO_SCHEMA_REMOVAL_DISABLED;
    }
}
