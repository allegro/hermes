package pl.allegro.tech.hermes.management.api.mappers;

import com.fasterxml.jackson.core.JsonParseException;
import pl.allegro.tech.hermes.api.ErrorCode;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
public class JsonParseExceptionMapper extends AbstractExceptionMapper<JsonParseException> {

    @Override
    Response.Status httpStatus() {
        return Response.Status.BAD_REQUEST;
    }

    @Override
    ErrorCode errorCode() {
        return ErrorCode.FORMAT_ERROR;
    }
}
