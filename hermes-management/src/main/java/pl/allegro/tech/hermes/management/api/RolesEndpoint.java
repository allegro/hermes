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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("/roles")
@Api(value = "/roles", description = "")
public class RolesEndpoint {

    @GET
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Get user roles", httpMethod = HttpMethod.GET)
    public Collection<String> getRoles(ContainerRequestContext requestContext) {
        return getRoles(requestContext, Collections.emptyList());
    }

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/topics/{topicName}")
    public Collection<String> getTopicRoles(ContainerRequestContext requestContext) {
        return getRoles(requestContext, Collections.singletonList(Roles.TOPIC_OWNER));
    }

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/{topicName}/subscriptions/{subscriptionName}")
    public Collection<String> getSubscriptionRoles(ContainerRequestContext requestContext) {
        return (new Random().nextBoolean()) ? getTopicRoles(requestContext) :
        getRoles(requestContext, Arrays.asList(Roles.TOPIC_OWNER, Roles.SUBSCRIPTION_OWNER));
    }

    private Collection<String> getRoles(ContainerRequestContext requestContext, Collection<String> additionalRoles) {
        SecurityContext securityContext = requestContext.getSecurityContext();
        Collection<String> roles = getBasicRoles(securityContext);
        for(String role : additionalRoles) {
            ifUserInRoleDo(securityContext, role, roles::add);
        }
        return roles;
    }

    private Collection<String> getBasicRoles(SecurityContext securityContext) {
        List<String> roles = new ArrayList<>();
        ifUserInRoleDo(securityContext, Roles.ADMIN, roles::add);
        ifUserInRoleDo(securityContext, Roles.ANY, roles::add);
        return roles;
    }

    private void ifUserInRoleDo(SecurityContext securityContext, String role, Consumer<String> consumer) {
        if (securityContext.isUserInRole(role)) {
            consumer.accept(role);
        }
    }


}
