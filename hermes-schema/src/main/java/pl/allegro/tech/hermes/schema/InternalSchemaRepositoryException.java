package pl.allegro.tech.hermes.schema;

import pl.allegro.tech.hermes.api.ErrorCode;

import javax.ws.rs.core.Response;

public class InternalSchemaRepositoryException extends SchemaException {

    public InternalSchemaRepositoryException(String subject, Response response) {
        this(subject, response.getStatus(), response.readEntity(String.class));
    }

    public InternalSchemaRepositoryException(String subject, int statusCode, String responseBody) {
        super(String.format("Internal schema repository error for subject %s request, server response: %d %s",
                subject, statusCode, responseBody));
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.SCHEMA_REPOSITORY_INTERNAL_ERROR;
    }
}
