package pl.allegro.tech.hermes.management.api.mappers;

import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import pl.allegro.tech.hermes.api.ErrorCode;

@Provider
public class NotSupportedExceptionMapper extends AbstractExceptionMapper<NotSupportedException> {

  @Override
  Response.Status httpStatus() {
    return Response.Status.UNSUPPORTED_MEDIA_TYPE;
  }

  @Override
  ErrorCode errorCode() {
    return ErrorCode.VALIDATION_ERROR;
  }
}
