package pl.allegro.tech.hermes.api.endpoints;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import pl.allegro.tech.hermes.api.MessageTextPreview;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicMetrics;
import pl.allegro.tech.hermes.api.TopicWithSchema;

import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static pl.allegro.tech.hermes.api.AvroMediaType.AVRO_BINARY;

@Path("topics")
public interface TopicEndpoint {
    @GET
    @Produces(APPLICATION_JSON)
    List<String> list(
            @DefaultValue("") @QueryParam("groupName") String groupName,
            @DefaultValue("false") @QueryParam("tracked") boolean tracked);

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Path("/query")
    List<String> queryList(
            @DefaultValue("") @QueryParam("groupName") String groupName,
            String query);

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    Response create(TopicWithSchema topic);

    @DELETE
    @Produces(APPLICATION_JSON)
    @Path("/{topicName}")
    Response remove(@PathParam("topicName") String qualifiedTopicName);

    @PUT
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Path("/{topicName}")
    Response update(@PathParam("topicName") String qualifiedTopicName, PatchData patch);

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/{topicName}")
    TopicWithSchema get(@PathParam("topicName") String qualifiedTopicName);

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/{topicName}/metrics")
    TopicMetrics getMetrics(@PathParam("topicName") String qualifiedTopicName);

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/{topicName}/preview")
    List<MessageTextPreview> getPreview(@PathParam("topicName") String qualifiedTopicName);

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Path("/{topicName}")
    Response publishMessage(@PathParam("topicName") String qualifiedTopicName, String message);

    @POST
    @Consumes(AVRO_BINARY)
    @Produces(APPLICATION_JSON)
    @Path("/{topicName}")
    Response publishMessage(@PathParam("topicName") String qualifiedTopicName, byte[] message);

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/{topicName}/preview/cluster/{brokersClusterName}/partition/{partition}/offset/{offset}")
    String preview(@PathParam("topicName") String qualifiedTopicName,
                   @PathParam("brokersClusterName") String brokersClusterName,
                   @PathParam("partition") Integer partition,
                   @PathParam("offset") Long offset);

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/owner/{ownerSourceName}/{ownerId}")
    List<Topic> listForOwner(@PathParam("ownerSourceName") String ownerSourceName, @PathParam("ownerId") String ownerId);

}
