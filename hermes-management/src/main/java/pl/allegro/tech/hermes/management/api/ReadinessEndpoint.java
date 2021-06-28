package pl.allegro.tech.hermes.management.api;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.Readiness;
import pl.allegro.tech.hermes.management.domain.readiness.DatacenterReadiness;
import pl.allegro.tech.hermes.management.domain.readiness.ReadinessService;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("readiness")
@Component
public class ReadinessEndpoint {

    private final ReadinessService readinessService;

    @Autowired
    public ReadinessEndpoint(ReadinessService readinessService) {
        this.readinessService = readinessService;
    }

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Path("/{datacenter}")
    public Response setReadiness(@PathParam("datacenter") String datacenter, Readiness readiness) {
        readinessService.setReady(new DatacenterReadiness(datacenter, readiness.isReady()));
        return Response.status(Response.Status.ACCEPTED).build();
    }
}