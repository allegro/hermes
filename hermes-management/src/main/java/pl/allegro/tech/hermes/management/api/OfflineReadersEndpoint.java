package pl.allegro.tech.hermes.management.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.management.domain.ManagementException;
import pl.allegro.tech.hermes.management.domain.readers.OfflineReadersService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("/topics")
public class OfflineReadersEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(OfflineReadersEndpoint.class);

    private final OfflineReadersService offlineReadersService;

    @Autowired(required = false)
    OfflineReadersEndpoint(OfflineReadersService offlineReadersService) {
        if (offlineReadersService == null) {
            logger.info("Offline readers bean is absent");
        }
        this.offlineReadersService = offlineReadersService;
    }

    @GET
    @Path("/{topic}/offline-readers")
    @Produces(APPLICATION_JSON)
    public Response find(@PathParam("topic") String topic) {
        if (offlineReadersService == null) {
            throw new OfflineReadersServiceAbsentException();
        } else {
            return Response.ok(offlineReadersService.find(TopicName.fromQualifiedName(topic))).build();
        }
    }

    private static class OfflineReadersServiceAbsentException extends ManagementException {

        OfflineReadersServiceAbsentException() {
            super("Offline readers implementation is absent");
        }

        @Override
        public ErrorCode getCode() {
            return ErrorCode.IMPLEMENTATION_ABSENT;
        }
    }
}
