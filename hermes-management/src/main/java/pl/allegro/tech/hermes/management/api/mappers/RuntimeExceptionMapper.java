package pl.allegro.tech.hermes.management.api.mappers;

import pl.allegro.tech.hermes.api.ErrorCode;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
public class RuntimeExceptionMapper extends AbstractExceptionMapper<RuntimeException> {

    @Override
    Response.Status httpStatus() {
        return Response.Status.INTERNAL_SERVER_ERROR;
    }

    @Override
    ErrorCode errorCode() {
        return ErrorCode.INTERNAL_ERROR;
    }
}
