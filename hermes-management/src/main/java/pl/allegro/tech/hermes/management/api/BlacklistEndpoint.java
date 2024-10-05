package pl.allegro.tech.hermes.management.api;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.status;
import static pl.allegro.tech.hermes.api.BlacklistStatus.BLACKLISTED;
import static pl.allegro.tech.hermes.api.BlacklistStatus.NOT_BLACKLISTED;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.BlacklistStatus;
import pl.allegro.tech.hermes.management.api.auth.HermesSecurityAwareRequestUser;
import pl.allegro.tech.hermes.management.api.auth.Roles;
import pl.allegro.tech.hermes.management.domain.auth.RequestUser;
import pl.allegro.tech.hermes.management.domain.blacklist.TopicBlacklistService;

@Component
@Path("/blacklist")
@Api(value = "/blacklist", description = "Operations on topics")
public class BlacklistEndpoint {

  private final TopicBlacklistService topicBlacklistService;

  @Autowired
  public BlacklistEndpoint(TopicBlacklistService topicBlacklistService) {
    this.topicBlacklistService = topicBlacklistService;
  }

  @GET
  @Produces(APPLICATION_JSON)
  @Path("/topics/{topicName}")
  @ApiOperation(value = "Is topic blacklisted", httpMethod = HttpMethod.GET)
  public BlacklistStatus isTopicBlacklisted(@PathParam("topicName") String qualifiedTopicName) {
    return topicBlacklistService.isBlacklisted(qualifiedTopicName) ? BLACKLISTED : NOT_BLACKLISTED;
  }

  @GET
  @Produces(APPLICATION_JSON)
  @Path("/topics")
  @RolesAllowed(Roles.ADMIN)
  public List<String> topicsBlacklist() {
    return topicBlacklistService.list();
  }

  @POST
  @Produces(APPLICATION_JSON)
  @Consumes(APPLICATION_JSON)
  @Path("/topics")
  @RolesAllowed(Roles.ADMIN)
  @ApiOperation(value = "Blacklist topics", httpMethod = HttpMethod.POST)
  public Response blacklistTopics(
      List<String> qualifiedTopicNames, @Context ContainerRequestContext requestContext) {
    RequestUser blacklistRequester = new HermesSecurityAwareRequestUser(requestContext);
    qualifiedTopicNames.forEach(
        topicName -> topicBlacklistService.blacklist(topicName, blacklistRequester));
    return status(Response.Status.OK).build();
  }

  @DELETE
  @Produces(APPLICATION_JSON)
  @Path("/topics/{topicName}")
  @RolesAllowed(Roles.ADMIN)
  @ApiOperation(value = "Unblacklist topic", httpMethod = HttpMethod.DELETE)
  public Response unblacklistTopic(
      @PathParam("topicName") String qualifiedTopicName,
      @Context ContainerRequestContext requestContext) {
    RequestUser unblacklistRequester = new HermesSecurityAwareRequestUser(requestContext);
    topicBlacklistService.unblacklist(qualifiedTopicName, unblacklistRequester);
    return status(Response.Status.OK).build();
  }
}
