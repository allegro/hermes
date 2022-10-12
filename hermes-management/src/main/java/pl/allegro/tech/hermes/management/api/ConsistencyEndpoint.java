package pl.allegro.tech.hermes.management.api;

import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.InconsistentGroup;
import pl.allegro.tech.hermes.management.api.auth.HermesSecurityAwareRequestUser;
import pl.allegro.tech.hermes.management.api.auth.Roles;
import pl.allegro.tech.hermes.management.domain.consistency.DcConsistencyService;
import pl.allegro.tech.hermes.management.domain.consistency.KafkaHermesConsistencyService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@RolesAllowed(Roles.ADMIN)
@Path("consistency")
public class ConsistencyEndpoint {
    private final DcConsistencyService dcConsistencyService;
    private final KafkaHermesConsistencyService kafkaHermesConsistencyService;

    public ConsistencyEndpoint(DcConsistencyService dcConsistencyService,
                               KafkaHermesConsistencyService kafkaHermesConsistencyService) {
        this.dcConsistencyService = dcConsistencyService;
        this.kafkaHermesConsistencyService = kafkaHermesConsistencyService;
    }

    @GET
    @Produces({APPLICATION_JSON})
    @Path("/inconsistencies/groups")
    public Response listInconsistentGroups(@QueryParam("groupNames") List<String> groupNames) {
        List<InconsistentGroup> inconsistentGroups = dcConsistencyService.listInconsistentGroups(new HashSet<>(groupNames));
        return Response.ok()
                .entity(new GenericEntity<List<InconsistentGroup>>(inconsistentGroups) {
                })
                .build();
    }

    @GET
    @Produces({APPLICATION_JSON})
    @Path("/inconsistencies/topics")
    public Response listInconsistentTopics() {
        return Response
                .ok(new GenericEntity<Set<String>>(kafkaHermesConsistencyService.listInconsistentTopics()) {
                })
                .build();
    }

    @DELETE
    @Produces({APPLICATION_JSON})
    @Path("/inconsistencies/topics")
    public Response removeTopicByName(@QueryParam("topicName") String topicName, @Context ContainerRequestContext requestContext) {
        kafkaHermesConsistencyService.removeTopic(topicName, new HermesSecurityAwareRequestUser(requestContext));
        return Response.ok().build();
    }

    @GET
    @Produces({APPLICATION_JSON})
    @Path("/groups")
    public Response listAllGroups() {
        Set<String> groupNames = dcConsistencyService.listAllGroupNames();
        return Response.ok()
                .entity(new GenericEntity<Set<String>>(groupNames) {
                })
                .build();
    }
}
