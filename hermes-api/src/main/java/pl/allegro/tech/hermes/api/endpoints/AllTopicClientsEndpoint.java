package pl.allegro.tech.hermes.api.endpoints;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("topics/{topic}/clients")
public interface AllTopicClientsEndpoint {

    @GET
    @Produces(APPLICATION_JSON)
    List<String> getTopicClients(@PathParam("topic") String topic);
}
