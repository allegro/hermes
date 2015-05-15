package pl.allegro.tech.hermes.management.api.mappers;

import pl.allegro.tech.hermes.api.ErrorCode;

import javax.ws.rs.NotSupportedException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

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
