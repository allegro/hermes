package pl.allegro.tech.hermes.management.api.mappers;

import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.management.api.auth.AuthException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

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