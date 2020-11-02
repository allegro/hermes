package pl.allegro.tech.hermes.management.api;

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

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@RolesAllowed(Roles.ADMIN)
@Path("consistency")
public class ConsistencyEndpoint {
    private final ConsistencyService consistencyService;

    public ConsistencyEndpoint(ConsistencyService consistencyService) {
        this.consistencyService = consistencyService;
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
    @Path("/groups")
    public Response listAllGroups() {
        Set<String> groupNames = consistencyService.listAllGroupNames();
        return Response.ok()
                .entity(new GenericEntity<Set<String>>(groupNames){})
                .build();
    }
}
