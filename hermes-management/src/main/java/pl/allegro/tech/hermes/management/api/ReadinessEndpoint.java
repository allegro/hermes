package pl.allegro.tech.hermes.management.api;

import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.DatacenterReadiness;
import pl.allegro.tech.hermes.api.Readiness;
import pl.allegro.tech.hermes.management.api.auth.Roles;
import pl.allegro.tech.hermes.management.domain.readiness.ReadinessService;

import java.util.List;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static pl.allegro.tech.hermes.api.DatacenterReadiness.ReadinessStatus.NOT_READY;
import static pl.allegro.tech.hermes.api.DatacenterReadiness.ReadinessStatus.READY;

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
    @RolesAllowed(Roles.ADMIN)
    @Path("/{datacenter}")
    public Response setReadiness(@PathParam("datacenter") String datacenter, Readiness readiness) {
        readinessService.setReady(new DatacenterReadiness(datacenter, readiness.isReady() ? READY : NOT_READY));
        return Response.status(Response.Status.ACCEPTED).build();
    }

    @GET
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed(Roles.ADMIN)
    public List<DatacenterReadiness> getReadiness() {
        return readinessService.getDatacentersReadinesses();
    }
}
