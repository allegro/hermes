package pl.allegro.tech.hermes.management.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.BlacklistStatus;
import pl.allegro.tech.hermes.management.api.auth.Roles;
import pl.allegro.tech.hermes.management.domain.blacklist.TopicBlacklistService;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.status;
import static pl.allegro.tech.hermes.api.BlacklistStatus.BLACKLISTED;
import static pl.allegro.tech.hermes.api.BlacklistStatus.NOT_BLACKLISTED;

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
    public Response blacklistTopics(List<String> qualifiedTopicNames) {
        qualifiedTopicNames.forEach(topicBlacklistService::blacklist);
        return status(Response.Status.OK).build();
    }

    @DELETE
    @Produces(APPLICATION_JSON)
    @Path("/topics/{topicName}")
    @RolesAllowed(Roles.ADMIN)
    @ApiOperation(value = "Unblacklist topic", httpMethod = HttpMethod.DELETE)
    public Response unblacklistTopic(@PathParam("topicName") String qualifiedTopicName) {
        topicBlacklistService.unblacklist(qualifiedTopicName);
        return status(Response.Status.OK).build();
    }
}
