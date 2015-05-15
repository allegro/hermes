package pl.allegro.tech.hermes.management.api.mappers;

import pl.allegro.tech.hermes.api.ErrorDescription;
import pl.allegro.tech.hermes.management.domain.ManagementException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ManagementExceptionMapper implements ExceptionMapper<ManagementException> {

    @Override
    public Response toResponse(ManagementException exception) {
        return Response
                .status(exception.getCode().getHttpCode())
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(new ErrorDescription(exception.getMessage(), exception.getCode()))
                .build();
    }
}
