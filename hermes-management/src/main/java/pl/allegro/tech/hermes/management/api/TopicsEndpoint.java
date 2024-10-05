package pl.allegro.tech.hermes.management.api;

import static com.google.common.base.Strings.isNullOrEmpty;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static jakarta.ws.rs.core.Response.status;
import static java.lang.String.format;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.MessageTextPreview;
import pl.allegro.tech.hermes.api.OwnerId;
import pl.allegro.tech.hermes.api.PatchData;
import pl.allegro.tech.hermes.api.Query;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicMetrics;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.api.TopicWithSchema;
import pl.allegro.tech.hermes.common.exception.BrokerNotFoundForPartitionException;
import pl.allegro.tech.hermes.management.api.auth.HermesSecurityAwareRequestUser;
import pl.allegro.tech.hermes.management.api.auth.ManagementRights;
import pl.allegro.tech.hermes.management.api.auth.Roles;
import pl.allegro.tech.hermes.management.domain.auth.RequestUser;
import pl.allegro.tech.hermes.management.domain.owner.OwnerSource;
import pl.allegro.tech.hermes.management.domain.owner.OwnerSourceNotFound;
import pl.allegro.tech.hermes.management.domain.owner.OwnerSources;
import pl.allegro.tech.hermes.management.domain.topic.CreatorRights;
import pl.allegro.tech.hermes.management.domain.topic.SingleMessageReaderException;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;

@Component
@Path("/topics")
@Api(value = "/topics", description = "Operations on topics")
public class TopicsEndpoint {

  private final TopicService topicService;
  private final ManagementRights managementRights;
  private final OwnerSources ownerSources;

  @Autowired
  public TopicsEndpoint(
      TopicService topicService, ManagementRights managementRights, OwnerSources ownerSources) {
    this.topicService = topicService;
    this.managementRights = managementRights;
    this.ownerSources = ownerSources;
  }

  @GET
  @Produces(APPLICATION_JSON)
  @ApiOperation(
      value = "List topics from group",
      response = List.class,
      httpMethod = HttpMethod.GET)
  public List<String> list(
      @DefaultValue("") @QueryParam("groupName") String groupName,
      @DefaultValue("false") @QueryParam("tracked") boolean tracked) {

    return tracked ? listTracked(groupName) : listNames(groupName);
  }

  @POST
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @Path("/query")
  @ApiOperation(
      value = "Queries topics from group",
      response = List.class,
      httpMethod = HttpMethod.POST)
  public List<String> queryList(
      @DefaultValue("") @QueryParam("groupName") String groupName, Query<Topic> query) {

    return isNullOrEmpty(groupName)
        ? topicService.listFilteredTopicNames(query)
        : topicService.listFilteredTopicNames(groupName, query);
  }

  @POST
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @RolesAllowed(Roles.ANY)
  @ApiOperation(value = "Create topic", httpMethod = HttpMethod.POST)
  public Response create(
      TopicWithSchema topicWithSchema, @Context ContainerRequestContext requestContext) {
    RequestUser requestUser = new HermesSecurityAwareRequestUser(requestContext);
    CreatorRights isAllowedToManage =
        checkedTopic -> managementRights.isUserAllowedToManageTopic(checkedTopic, requestContext);
    topicService.createTopicWithSchema(topicWithSchema, requestUser, isAllowedToManage);
    return status(Response.Status.CREATED).build();
  }

  @DELETE
  @Produces(APPLICATION_JSON)
  @Path("/{topicName}")
  @RolesAllowed({Roles.ADMIN, Roles.TOPIC_OWNER})
  @ApiOperation(value = "Remove topic", httpMethod = HttpMethod.DELETE)
  public Response remove(
      @PathParam("topicName") String qualifiedTopicName,
      @Context ContainerRequestContext requestContext) {
    RequestUser requestUser = new HermesSecurityAwareRequestUser(requestContext);
    topicService.removeTopicWithSchema(
        topicService.getTopicDetails(TopicName.fromQualifiedName(qualifiedTopicName)), requestUser);
    return status(Response.Status.OK).build();
  }

  @PUT
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  @Path("/{topicName}")
  @RolesAllowed({Roles.ADMIN, Roles.TOPIC_OWNER})
  @ApiOperation(value = "Update topic", httpMethod = HttpMethod.PUT)
  public Response update(
      @PathParam("topicName") String qualifiedTopicName,
      PatchData patch,
      @Context ContainerRequestContext requestContext) {
    RequestUser requestUser = new HermesSecurityAwareRequestUser(requestContext);
    topicService.updateTopicWithSchema(
        TopicName.fromQualifiedName(qualifiedTopicName), patch, requestUser);
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
  public byte[] getPreviewRaw(
      @PathParam("topicName") String qualifiedTopicName, @PathParam("idx") Integer idx) {
    TopicName topicName = TopicName.fromQualifiedName(qualifiedTopicName);
    Optional<byte[]> preview = topicService.preview(topicName, idx);
    if (preview.isPresent()) {
      return preview.get();
    } else {
      throw new NotFoundException(
          format("Message preview not found for topic %s and offset %d", topicName, idx));
    }
  }

  @GET
  @Produces(APPLICATION_JSON)
  @Path("/{topicName}/preview/cluster/{brokersClusterName}/partition/{partition}/offset/{offset}")
  @RolesAllowed({Roles.ADMIN})
  @ApiOperation(
      value = "Fetch single message from specified brokers cluster",
      httpMethod = HttpMethod.GET)
  public String preview(
      @PathParam("topicName") String qualifiedTopicName,
      @PathParam("brokersClusterName") String brokersClusterName,
      @PathParam("partition") Integer partition,
      @PathParam("offset") Long offset) {
    try {
      return topicService.fetchSingleMessageFromPrimary(
          brokersClusterName, TopicName.fromQualifiedName(qualifiedTopicName), partition, offset);
    } catch (BrokerNotFoundForPartitionException | SingleMessageReaderException exception) {
      throw new NotFoundException(
          format(
              "Message not found for brokers cluster %s, topic %s, partition %d and offset %d",
              brokersClusterName, qualifiedTopicName, partition, offset));
    }
  }

  @GET
  @Produces(APPLICATION_JSON)
  @Path("/owner/{ownerSourceName}/{ownerId}")
  public List<Topic> listForOwner(
      @PathParam("ownerSourceName") String ownerSourceName, @PathParam("ownerId") String id) {
    OwnerSource ownerSource =
        ownerSources
            .getByName(ownerSourceName)
            .orElseThrow(() -> new OwnerSourceNotFound(ownerSourceName));
    if (!ownerSource.exists(id)) {
      throw new OwnerSource.OwnerNotFound(ownerSourceName, id);
    }
    OwnerId ownerId = new OwnerId(ownerSource.name(), id);
    return topicService.listForOwnerId(ownerId);
  }

  private List<String> listTracked(String groupName) {
    return isNullOrEmpty(groupName)
        ? topicService.listTrackedTopicNames()
        : topicService.listTrackedTopicNames(groupName);
  }

  private List<String> listNames(String groupName) {
    return isNullOrEmpty(groupName)
        ? topicService.listQualifiedTopicNames()
        : topicService.listQualifiedTopicNames(groupName);
  }
}
