package pl.allegro.tech.hermes.management.api;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.management.api.auth.Roles;

import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("/roles")
@Api(value = "/roles", description = "")
public class RolesEndpoint {

    @GET
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get user roles", httpMethod = HttpMethod.GET)
    public Collection<String> getRoles(ContainerRequestContext requestContext) {
        SecurityContext securityContext = requestContext.getSecurityContext();
        List<String> roles = new ArrayList<>();
        if(securityContext.isUserInRole(Roles.ADMIN)) {
            roles.add(Roles.ADMIN);
        }
        return roles;
    }

}
