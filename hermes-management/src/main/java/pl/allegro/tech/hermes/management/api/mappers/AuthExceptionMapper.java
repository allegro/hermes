package pl.allegro.tech.hermes.management.api.mappers;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.management.api.auth.AuthException;

@Provider
public class AuthExceptionMapper extends AbstractExceptionMapper<AuthException> {

  @Override
  Response.Status httpStatus() {
    return Response.Status.FORBIDDEN;
  }

  @Override
  ErrorCode errorCode() {
    return ErrorCode.AUTH_ERROR;
  }
}
