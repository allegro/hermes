package pl.allegro.tech.hermes.schema;

import pl.allegro.tech.hermes.api.ErrorCode;

import javax.ws.rs.core.Response;

public class BadSchemaRequestException extends SchemaException {

    public BadSchemaRequestException(String subject, Response response) {
        this(subject, response.getStatus(), response.readEntity(String.class));
    }

    public BadSchemaRequestException(String subject, int statusCode, String responseBody) {
        super(String.format("Bad schema request for subject %s, server response: %d %s",
                subject, statusCode, responseBody));
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.SCHEMA_BAD_REQUEST;
    }
}
