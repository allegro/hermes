package pl.allegro.tech.hermes.management;

import org.apache.commons.lang.NotImplementedException;
import pl.allegro.tech.hermes.management.api.auth.SecurityProvider;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

public class TestSecurityProvider implements SecurityProvider {

    private static volatile boolean userIsAdmin = true;
    private static volatile boolean isOwner = true;

    public static synchronized void setUserIsAdmin(boolean userIsAdmin) {
        TestSecurityProvider.userIsAdmin = userIsAdmin;
    }

    public static synchronized void setIsOwner(boolean userIsOwner) {
        TestSecurityProvider.isOwner = userIsOwner;
    }

    @Override
    public HermesSecurity security(ContainerRequestContext requestContext) {
        return new HermesSecurity(securityContext(requestContext), ownerId -> isOwner);
    }

    private SecurityContext securityContext(ContainerRequestContext requestContext) {

        return new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return new TestUserPrincipal();
            }

            @Override
            public boolean isUserInRole(String role) {
                return checkRoles(role);
            }

            @Override
            public boolean isSecure() {
                throw new NotImplementedException();
            }

            @Override
            public String getAuthenticationScheme() {
                throw new NotImplementedException();
            }
        };
    }

    private synchronized boolean checkRoles(String role) {
        if (role.equalsIgnoreCase("admin")) {
            return userIsAdmin;
        } else {
            return true;
        }
    }

    private static class TestUserPrincipal implements Principal {

        @Override
        public String getName() {
            return "test-user";
        }
    }
}
