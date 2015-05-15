package pl.allegro.tech.hermes.management.api.mappers;

import com.fasterxml.jackson.databind.JsonMappingException;
import pl.allegro.tech.hermes.api.ErrorCode;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

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
