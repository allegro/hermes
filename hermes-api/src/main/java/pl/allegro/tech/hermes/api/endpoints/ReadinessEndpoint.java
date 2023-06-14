package pl.allegro.tech.hermes.api.endpoints;

import pl.allegro.tech.hermes.api.DatacenterReadiness;
import pl.allegro.tech.hermes.api.Readiness;

import java.util.List;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("readiness/datacenters")
public interface ReadinessEndpoint {
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Path("/{datacenter}")
    Response setReadiness(@PathParam("datacenter") String datacenter, Readiness readiness);

    @GET
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    List<DatacenterReadiness> getReadiness();
}
