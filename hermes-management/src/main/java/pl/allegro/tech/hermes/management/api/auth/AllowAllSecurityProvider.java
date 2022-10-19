package pl.allegro.tech.hermes.management.api.auth;

import org.apache.commons.lang.NotImplementedException;

import java.security.Principal;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;

public class AllowAllSecurityProvider implements SecurityProvider {

    @Override
    public HermesSecurity security(ContainerRequestContext requestContext) {
        return new HermesSecurity(securityContext(requestContext), ownerId -> true);
    }

    private SecurityContext securityContext(ContainerRequestContext requestContext) {
        return new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return new AnonymousUserPrincipal();
            }

            @Override
            public boolean isUserInRole(String role) {
                return true;
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

    private static class AnonymousUserPrincipal implements Principal {

        @Override
        public String getName() {
            return "[anonymous user]";
        }
    }
}
