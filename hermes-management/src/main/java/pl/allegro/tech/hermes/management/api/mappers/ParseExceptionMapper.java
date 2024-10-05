package pl.allegro.tech.hermes.management.api.mappers;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.management.infrastructure.query.parser.ParseException;

@Provider
public class ParseExceptionMapper extends AbstractExceptionMapper<ParseException> {

  @Override
  Response.Status httpStatus() {
    return Response.Status.BAD_REQUEST;
  }

  @Override
  ErrorCode errorCode() {
    return ErrorCode.FORMAT_ERROR;
  }
}
