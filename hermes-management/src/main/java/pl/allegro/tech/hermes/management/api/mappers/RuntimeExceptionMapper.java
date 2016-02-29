package pl.allegro.tech.hermes.management.api.mappers;

import com.google.common.base.Throwables;
import org.boon.Maps;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class RuntimeExceptionMapper implements ExceptionMapper<RuntimeException> {

    @Override
    public Response toResponse(RuntimeException exception) {
        return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(Maps.map(
                        "message", exception.getMessage(),
                        "details", Throwables.getStackTraceAsString(exception)
                ))
                .build();
    }
}
