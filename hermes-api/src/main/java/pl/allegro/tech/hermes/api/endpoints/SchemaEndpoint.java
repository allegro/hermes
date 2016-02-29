package pl.allegro.tech.hermes.api.endpoints;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("topics/{topicName}/schema")
public interface SchemaEndpoint {

    @GET
    @Produces(APPLICATION_JSON)
    Response get(@PathParam("topicName") String qualifiedTopicName);

    @GET
    @Produces(APPLICATION_JSON)
    Response get(@PathParam("topicName") String qualifiedTopicName, int version);

    @POST
    @Consumes(APPLICATION_JSON)
    Response save(@PathParam("topicName") String qualifiedTopicName, String schema);

    @DELETE
    Response delete(@PathParam("topicName") String qualifiedTopicName);
}
