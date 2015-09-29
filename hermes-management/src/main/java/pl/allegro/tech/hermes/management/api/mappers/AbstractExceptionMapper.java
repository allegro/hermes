package pl.allegro.tech.hermes.management.api.mappers;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.ErrorDescription;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

abstract class AbstractExceptionMapper<T extends Throwable> implements ExceptionMapper<T> {

    @Override
    public Response toResponse(T exception) {
        return Response
            .status(httpStatus())
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
