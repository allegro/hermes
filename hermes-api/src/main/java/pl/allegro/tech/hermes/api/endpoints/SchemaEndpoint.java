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
    @Path("versions/{version}")
    @Produces(APPLICATION_JSON)
    Response getByVersion(@PathParam("topicName") String qualifiedTopicName, @PathParam("version") int version);

    @GET
    @Path("ids/{id}")
    @Produces(APPLICATION_JSON)
    Response getById(@PathParam("topicName") String qualifiedTopicName, @PathParam("id") int id);

    @POST
    @Consumes(APPLICATION_JSON)
    Response save(@PathParam("topicName") String qualifiedTopicName, String schema);

    @POST
    @Consumes(APPLICATION_JSON)
    Response save(@PathParam("topicName") String qualifiedTopicName, @QueryParam(value = "validate") boolean validate, String schema);

    @DELETE
    Response delete(@PathParam("topicName") String qualifiedTopicName);
}
