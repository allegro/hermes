package pl.allegro.tech.hermes.management.api.mappers;

import pl.allegro.tech.hermes.api.ErrorCode;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
public class IllegalArgumentExceptionMapper extends AbstractExceptionMapper<IllegalArgumentException> {

    @Override
    Response.Status httpStatus() {
        return Response.Status.BAD_REQUEST;
    }

    @Override
    ErrorCode errorCode() {
        return ErrorCode.OTHER;
    }
}
