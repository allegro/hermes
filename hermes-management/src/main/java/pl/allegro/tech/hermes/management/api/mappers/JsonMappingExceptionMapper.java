package pl.allegro.tech.hermes.management.api.mappers;

import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import pl.allegro.tech.hermes.api.ErrorCode;

@Provider
public class JsonMappingExceptionMapper extends AbstractExceptionMapper<JsonMappingException> {

  @Override
  Response.Status httpStatus() {
    return Response.Status.BAD_REQUEST;
  }

  @Override
  ErrorCode errorCode() {
    return ErrorCode.FORMAT_ERROR;
  }
}
