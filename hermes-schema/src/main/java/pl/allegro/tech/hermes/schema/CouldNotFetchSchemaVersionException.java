package pl.allegro.tech.hermes.schema;

import pl.allegro.tech.hermes.api.ErrorCode;

public class CouldNotFetchSchemaVersionException extends SchemaException {

    public CouldNotFetchSchemaVersionException(String subject, String version, int responseStatus, String responseBody) {
        super(String.format("Could not fetch schema for subject %s at version %s, reason: %d %s",
                subject, version, responseStatus, responseBody));
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.SCHEMA_COULD_NOT_BE_LOADED;
    }
}
