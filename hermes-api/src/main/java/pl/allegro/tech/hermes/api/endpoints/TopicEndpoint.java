package pl.allegro.tech.hermes.api.endpoints;

import pl.allegro.tech.hermes.api.MessageTextPreview;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicMetrics;
import pl.allegro.tech.hermes.api.TopicWithSchema;

import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
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
