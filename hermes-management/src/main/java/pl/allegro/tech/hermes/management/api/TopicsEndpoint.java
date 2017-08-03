package pl.allegro.tech.hermes.management.api;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.MessageTextPreview;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.api.Query;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicWithSchema;
import pl.allegro.tech.hermes.api.TopicMetrics;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.exception.BrokerNotFoundForPartitionException;
import pl.allegro.tech.hermes.management.api.auth.ManagementRights;
import pl.allegro.tech.hermes.management.api.auth.Roles;
import pl.allegro.tech.hermes.management.domain.topic.CreatorRights;
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
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static javax.ws.rs.core.Response.status;

@Component
@Path("/topics")
@Api(value = "/topics", description = "Operations on topics")
public class TopicsEndpoint {

    private final TopicService topicService;
    private final ManagementRights managementRights;

    @Autowired
    public TopicsEndpoint(TopicService topicService,
                          ManagementRights managementRights) {
        this.topicService = topicService;
        this.managementRights = managementRights;
    }

    @GET
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "List topics from group", response = List.class, httpMethod = HttpMethod.GET)
    public List<String> list(
            @DefaultValue("") @QueryParam("groupName") String groupName,
            @DefaultValue("false") @QueryParam("tracked") boolean tracked) {

        return tracked ? listTracked(groupName) : listNames(groupName);
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
    @RolesAllowed(Roles.ANY)
    @ApiOperation(value = "Create topic", httpMethod = HttpMethod.POST)
    public Response create(TopicWithSchema topicWithSchema, @Context ContainerRequestContext requestContext) {
        String createdBy = requestContext.getSecurityContext().getUserPrincipal().getName();
        CreatorRights isAllowedToManage = checkedTopic -> managementRights.isUserAllowedToManageTopic(checkedTopic, requestContext);
        topicService.createTopicWithSchema(topicWithSchema, createdBy, isAllowedToManage);
        return status(Response.Status.CREATED).build();
    }

    @DELETE
    @Produces(APPLICATION_JSON)
    @Path("/{topicName}")
    @RolesAllowed({Roles.TOPIC_OWNER, Roles.ADMIN})
    @ApiOperation(value = "Remove topic", httpMethod = HttpMethod.DELETE)
    public Response remove(@PathParam("topicName") String qualifiedTopicName, @Context SecurityContext securityContext) {
        topicService.removeTopicWithSchema(topicService.getTopicDetails(TopicName.fromQualifiedName(qualifiedTopicName)),
                securityContext.getUserPrincipal().getName());
        return status(Response.Status.OK).build();
    }

    @PUT
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Path("/{topicName}")
    @RolesAllowed({Roles.TOPIC_OWNER, Roles.ADMIN})
    @ApiOperation(value = "Update topic", httpMethod = HttpMethod.PUT)
    public Response update(@PathParam("topicName") String qualifiedTopicName, PatchData patch,
                           @Context SecurityContext securityContext) {
        String updatedBy = securityContext.getUserPrincipal().getName();
        topicService.updateTopicWithSchema(TopicName.fromQualifiedName(qualifiedTopicName), patch, updatedBy);
        return status(Response.Status.OK).build();
    }

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/{topicName}")
    @ApiOperation(value = "Topic details", httpMethod = HttpMethod.GET)
    public TopicWithSchema get(@PathParam("topicName") String qualifiedTopicName) {
        return topicService.getTopicWithSchema(TopicName.fromQualifiedName(qualifiedTopicName));
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
    @Path("/{topicName}/preview")
    @ApiOperation(value = "Topic publisher preview", httpMethod = HttpMethod.GET)
    public List<MessageTextPreview> getPreview(@PathParam("topicName") String qualifiedTopicName) {
        return topicService.previewText(TopicName.fromQualifiedName(qualifiedTopicName));
    }

    @GET
    @Produces(APPLICATION_OCTET_STREAM)
    @Path("/{topicName}/preview/{idx}")
    @ApiOperation(value = "Topic publisher preview", httpMethod = HttpMethod.GET)
    public byte[] getPreviewRaw(@PathParam("topicName") String qualifiedTopicName, @PathParam("idx") Integer idx) {
        TopicName topicName = TopicName.fromQualifiedName(qualifiedTopicName);
        Optional<byte[]> preview = topicService.preview(topicName, idx);
        if (preview.isPresent()) {
            return preview.get();
        } else {
            throw new NotFoundException(format(
                    "Message preview not found for topic %s and offset %d",
                    topicName, idx
            ));
        }
    }

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/{topicName}/preview/cluster/{brokersClusterName}/partition/{partition}/offset/{offset}")
    @RolesAllowed({Roles.ADMIN})
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
