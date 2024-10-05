package pl.allegro.tech.hermes.management.api.mappers;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import pl.allegro.tech.hermes.api.ErrorCode;

@Provider
public class IOExceptionMapper extends AbstractExceptionMapper<IOException> {

  @Override
  Response.Status httpStatus() {
    return Response.Status.BAD_REQUEST;
  }

  @Override
  ErrorCode errorCode() {
    return ErrorCode.OTHER;
  }
}
