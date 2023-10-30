package pl.allegro.tech.hermes.api.endpoints;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import pl.allegro.tech.hermes.api.Stats;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("stats")
public interface StatsEndpoint {
    @GET
    @Produces(APPLICATION_JSON)
    Stats getStats();
}
