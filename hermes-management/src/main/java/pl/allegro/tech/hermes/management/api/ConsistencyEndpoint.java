package pl.allegro.tech.hermes.management.api;

import javax.ws.rs.DELETE;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.InconsistentGroup;
import pl.allegro.tech.hermes.management.api.auth.Roles;
import pl.allegro.tech.hermes.management.domain.consistency.ConsistencyService;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import pl.allegro.tech.hermes.management.domain.consistency.TopicConsistencyService;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@RolesAllowed(Roles.ADMIN)
@Path("consistency")
public class ConsistencyEndpoint {
    private final ConsistencyService consistencyService;
    private final TopicConsistencyService topicConsistencyService;

    public ConsistencyEndpoint(ConsistencyService consistencyService,
        TopicConsistencyService topicConsistencyService) {
        this.consistencyService = consistencyService;
        this.topicConsistencyService = topicConsistencyService;
    }

    @GET
    @Produces({APPLICATION_JSON})
    @Path("/inconsistencies/groups")
    public Response listInconsistentGroups(@QueryParam("groupNames") List<String> groupNames) {
        List<InconsistentGroup> inconsistentGroups = consistencyService.listInconsistentGroups(new HashSet<>(groupNames));
        return Response.ok()
                .entity(new GenericEntity<List<InconsistentGroup>>(inconsistentGroups){})
                .build();
    }

    @GET
    @Produces({APPLICATION_JSON})
    @Path("/inconsistencies/topics")
    public Response listInconsistentTopics() {
        return Response
            .ok(new GenericEntity<Set<String>>(topicConsistencyService.listInconsistentTopics()){})
            .build();
    }

    @DELETE
    @Produces({APPLICATION_JSON})
    @Path("/inconsistencies/topics")
    public Response removeTopicByName(@QueryParam("topicName") String topicName) {
        topicConsistencyService.removeTopic(topicName);
        return Response.ok().build();
    }

    @GET
    @Produces({APPLICATION_JSON})
    @Path("/groups")
    public Response listAllGroups() {
        Set<String> groupNames = consistencyService.listAllGroupNames();
        return Response.ok()
                .entity(new GenericEntity<Set<String>>(groupNames){})
                .build();
    }
}
