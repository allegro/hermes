package pl.allegro.tech.hermes.management.api.mappers;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.ErrorDescription;

@Provider
public class RuntimeExceptionMapper implements ExceptionMapper<RuntimeException> {

  private final Logger logger = LoggerFactory.getLogger(RuntimeExceptionMapper.class);

  @Override
  public Response toResponse(RuntimeException exception) {
    logger.warn("Caught unmapped exception: {}", exception.getClass().getSimpleName(), exception);
    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
        .type(MediaType.APPLICATION_JSON_TYPE)
        .entity(new ErrorDescription(exception.getMessage(), ErrorCode.OTHER))
        .build();
  }
}
