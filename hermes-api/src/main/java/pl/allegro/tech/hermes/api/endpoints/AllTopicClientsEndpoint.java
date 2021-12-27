package pl.allegro.tech.hermes.api.endpoints;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("topics/{topic}/clients")
public interface AllTopicClientsEndpoint {

    @GET
    @Produces(APPLICATION_JSON)
    List<String> getTopicClients(@PathParam("topic") String topic);
}
