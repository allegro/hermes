package pl.allegro.tech.hermes.schema;

import pl.allegro.tech.hermes.api.ErrorCode;

import javax.ws.rs.core.Response;

public class CouldNotFetchSchemaVersionsException extends SchemaException {

    public CouldNotFetchSchemaVersionsException(String subject, Response response) {
        super(String.format("Could not fetch schema versions for subject %s, reason: %d %s",
                subject, response.getStatus(), response.readEntity(String.class)));
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.SCHEMA_COULD_NOT_BE_LOADED;
    }
}
