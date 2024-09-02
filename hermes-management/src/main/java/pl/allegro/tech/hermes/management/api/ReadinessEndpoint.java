package pl.allegro.tech.hermes.management.api;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static pl.allegro.tech.hermes.api.DatacenterReadiness.ReadinessStatus.NOT_READY;
import static pl.allegro.tech.hermes.api.DatacenterReadiness.ReadinessStatus.READY;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.DatacenterReadiness;
import pl.allegro.tech.hermes.api.Readiness;
import pl.allegro.tech.hermes.management.api.auth.Roles;
import pl.allegro.tech.hermes.management.domain.readiness.ReadinessService;

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
    readinessService.setReady(
        new DatacenterReadiness(datacenter, readiness.isReady() ? READY : NOT_READY));
    return Response.status(Response.Status.ACCEPTED).build();
  }

  @GET
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @RolesAllowed(Roles.ADMIN)
  public List<DatacenterReadiness> getReadiness() {
    return readinessService.getDatacentersReadiness();
  }
}
