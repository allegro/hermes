package pl.allegro.tech.hermes.schema;

import pl.allegro.tech.hermes.api.ErrorCode;

import javax.ws.rs.core.Response;

public class CouldNotFetchSchemaVersionException extends SchemaException {

    public CouldNotFetchSchemaVersionException(String subject, String version, Response response) {
        super(String.format("Could not fetch schema for subject %s at version %s, reason: %d %s",
                subject, version, response.getStatus(), response.readEntity(String.class)));
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.SCHEMA_COULD_NOT_BE_LOADED;
    }
}
