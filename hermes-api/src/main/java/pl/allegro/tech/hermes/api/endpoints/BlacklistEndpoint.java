package pl.allegro.tech.hermes.api.endpoints;

import pl.allegro.tech.hermes.api.BlacklistStatus;

import java.util.List;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("blacklist")
public interface BlacklistEndpoint {

    @POST
    @Produces(APPLICATION_JSON)
    @Consumes(APPLICATION_JSON)
    @Path("/topics")
    Response blacklistTopics(List<String> qualifiedTopicNames);

    @DELETE
    @Produces(APPLICATION_JSON)
    @Path("/topics/{topicName}")
    Response unblacklistTopic(@PathParam("topicName") String qualifiedTopicName);

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/topics/{topicName}")
    BlacklistStatus isTopicBlacklisted(@PathParam("topicName") String qualifiedTopicName);

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/topics")
    List<String> topicsBlacklist();
}
