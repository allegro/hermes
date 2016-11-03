package pl.allegro.tech.hermes.schema;

import pl.allegro.tech.hermes.api.ErrorCode;

import javax.ws.rs.core.Response;

public class CouldNotRegisterSchemaSubjectException extends SchemaException {

    public CouldNotRegisterSchemaSubjectException(String subject, Response response) {
        super(String.format("Could not register subject %s in schema repository, reason: %d %s",
                subject, response.getStatus(), response.readEntity(String.class)));
    }

    @Override
    public ErrorCode getCode() {
        return ErrorCode.SCHEMA_COULD_NOT_BE_SAVED;
    }
}
