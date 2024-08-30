package pl.allegro.tech.hermes.management.api.mappers;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import pl.allegro.tech.hermes.api.ErrorDescription;
import pl.allegro.tech.hermes.schema.SchemaException;

@Provider
public class SchemaExceptionMapper implements ExceptionMapper<SchemaException> {

  @Override
  public Response toResponse(SchemaException exception) {
    return Response.status(exception.getCode().getHttpCode())
        .type(MediaType.APPLICATION_JSON_TYPE)
        .entity(new ErrorDescription(exception.getMessage(), exception.getCode()))
        .build();
  }
}
