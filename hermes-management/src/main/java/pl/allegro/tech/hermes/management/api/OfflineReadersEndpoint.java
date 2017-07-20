package pl.allegro.tech.hermes.management.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.management.domain.readers.OfflineReadersService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("/readers")
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
    @Path("/{topic}")
    @Produces(APPLICATION_JSON)
    public Response find(@PathParam("topic") String topic) {
        if (offlineReadersService == null) {
            logger.warn("Offline readers bean is absent");
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            return Response.ok(offlineReadersService.find(topic)).build();
        }
    }
}
