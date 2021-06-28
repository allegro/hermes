package pl.allegro.tech.hermes.api.endpoints;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import pl.allegro.tech.hermes.api.Readiness;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("readiness")
public interface ReadinessEndpoint {
    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Path("/{datacenter}")
    Response setReadiness(@PathParam("datacenter") String datacenter, Readiness readiness);
}
