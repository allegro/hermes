package pl.allegro.tech.hermes.api.endpoints;

import pl.allegro.tech.hermes.api.BlacklistStatus;

import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

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
