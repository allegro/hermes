package pl.allegro.tech.hermes.api.endpoints;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("topics")
public interface AllTopicClientsEndpoint {

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/{topic}/clients")
    String getTopicClients(@PathParam("topic") String topic);
}
