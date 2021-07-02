package pl.allegro.tech.hermes.management.api;

import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.Readiness;
import pl.allegro.tech.hermes.api.DatacenterReadiness;
import pl.allegro.tech.hermes.management.domain.readiness.ReadinessService;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("readiness/datacenters")
@Component
public class ReadinessEndpoint {

    private final ReadinessService readinessService;

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

    @GET
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public List<DatacenterReadiness> getReadiness() {
        return readinessService.getDatacentersReadinesses();
    }
}