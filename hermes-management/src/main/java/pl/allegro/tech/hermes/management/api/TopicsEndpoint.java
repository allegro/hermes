package pl.allegro.tech.hermes.management.api;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicMetrics;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.exception.BrokerNotFoundForPartitionException;
import pl.allegro.tech.hermes.common.query.Query;
import pl.allegro.tech.hermes.management.api.auth.Roles;
import pl.allegro.tech.hermes.management.api.validator.ApiPreconditions;
import pl.allegro.tech.hermes.management.config.TopicProperties;
import pl.allegro.tech.hermes.management.domain.topic.SingleMessageReaderException;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.status;
import static pl.allegro.tech.hermes.api.Topic.Builder.topic;

@Component
@Path("/topics")
@Api(value = "/topics", description = "Operations on topics")
public class TopicsEndpoint {

    private final TopicService topicService;
    private final ApiPreconditions preconditions;
    private final TopicProperties topicProperties;

    @Autowired
    public TopicsEndpoint(TopicService topicService, ApiPreconditions preconditions, TopicProperties topicProperties) {
        this.topicService = topicService;
        this.preconditions = preconditions;
        this.topicProperties = topicProperties;
    }

    @GET
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "List topics from group", response = List.class, httpMethod = HttpMethod.GET)
    public List<String> list(
            @DefaultValue("") @QueryParam("groupName") String groupName,
            @DefaultValue("false") @QueryParam("tracked") boolean tracked) {

        return tracked? listTracked(groupName) : listNames(groupName);
    }

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Path("/query")
    @ApiOperation(value = "Queries topics from group", response = List.class, httpMethod = HttpMethod.POST)
    public List<String> queryList(
            @DefaultValue("") @QueryParam("groupName") String groupName,
            Query<Topic> query) {

        return isNullOrEmpty(groupName) ?
                topicService.listFilteredTopicNames(query) :
                topicService.listFilteredTopicNames(groupName, query);
    }

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @RolesAllowed({ Roles.GROUP_OWNER, Roles.ADMIN })
    @ApiOperation(value = "Create topic", httpMethod = HttpMethod.POST)
    public Response create(Topic topic) {
        preconditions.checkConstraints(topic);
        topicService.createTopic(topic().applyDefaults().withContentType(topicProperties.getDefaultContentType()).applyPatch(topic).build());
        return status(Response.Status.CREATED).build();
    }

    @DELETE
    @Produces(APPLICATION_JSON)
    @Path("/{topicName}")
    @RolesAllowed({ Roles.GROUP_OWNER, Roles.ADMIN })
    @ApiOperation(value = "Remove topic", httpMethod = HttpMethod.DELETE)
    public Response remove(@PathParam("topicName") String qualifiedTopicName) {
        topicService.removeTopic(topicService.getTopicDetails(TopicName.fromQualifiedName(qualifiedTopicName)));
        return status(Response.Status.OK).build();
    }

    @PUT
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Path("/{topicName}")
    @RolesAllowed({ Roles.GROUP_OWNER, Roles.ADMIN })
    @ApiOperation(value = "Update topic", httpMethod = HttpMethod.PUT)
    public Response update(@PathParam("topicName") String qualifiedTopicName, Topic update) {
        topicService.updateTopic(topic().applyPatch(update).withName(qualifiedTopicName).build());
        return status(Response.Status.OK).build();
    }

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/{topicName}")
    @ApiOperation(value = "Topic details", httpMethod = HttpMethod.GET)
    public Topic get(@PathParam("topicName") String qualifiedTopicName) {
        return topicService.getTopicDetails(TopicName.fromQualifiedName(qualifiedTopicName));
    }

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/{topicName}/metrics")
    @ApiOperation(value = "Topic metrics", httpMethod = HttpMethod.GET)
    public TopicMetrics getMetrics(@PathParam("topicName") String qualifiedTopicName) {
        return topicService.getTopicMetrics(TopicName.fromQualifiedName(qualifiedTopicName));
    }

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/{topicName}/preview/cluster/{brokersClusterName}/partition/{partition}/offset/{offset}")
    @RolesAllowed({ Roles.ADMIN })
    @ApiOperation(value = "Fetch single message from specified brokers cluster", httpMethod = HttpMethod.GET)
    public String preview(@PathParam("topicName") String qualifiedTopicName,
                          @PathParam("brokersClusterName") String brokersClusterName,
                          @PathParam("partition") Integer partition,
                          @PathParam("offset") Long offset) {
        try {
            return topicService.fetchSingleMessageFromPrimary(brokersClusterName, TopicName.fromQualifiedName(qualifiedTopicName), partition, offset);
        } catch (BrokerNotFoundForPartitionException | SingleMessageReaderException exception) {
            throw new NotFoundException(format(
                "Message not found for brokers cluster %s, topic %s, partition %d and offset %d",
                brokersClusterName, qualifiedTopicName, partition, offset
            ));
        }
    }

    private List<String> listTracked(String groupName) {
        return isNullOrEmpty(groupName) ? topicService.listTrackedTopicNames() : topicService.listTrackedTopicNames(groupName);
    }

    private List<String> listNames(String groupName) {
        return isNullOrEmpty(groupName) ? topicService.listQualifiedTopicNames() : topicService.listQualifiedTopicNames(groupName);
    }
}
