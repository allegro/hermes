package pl.allegro.tech.hermes.management.api.mappers;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.ErrorDescription;

abstract class AbstractExceptionMapper<T extends Throwable> implements ExceptionMapper<T> {

  @Override
  public Response toResponse(T exception) {
    return Response.status(httpStatus())
        .type(MediaType.APPLICATION_JSON_TYPE)
        .entity(new ErrorDescription(errorMessage(exception), errorCode()))
        .build();
  }

  String errorMessage(T exception) {
    return exception.getMessage();
  }

  abstract Response.Status httpStatus();

  abstract ErrorCode errorCode();
}
