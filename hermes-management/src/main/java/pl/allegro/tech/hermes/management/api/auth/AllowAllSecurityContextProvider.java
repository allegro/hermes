package pl.allegro.tech.hermes.management.api.auth;

import org.apache.commons.lang.NotImplementedException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

public class AllowAllSecurityContextProvider implements SecurityContextProvider {
    
    @Override
    public SecurityContext securityContext(ContainerRequestContext requestContext) {
        return new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return new Principal() {
                    @Override
                    public String getName() {
                        return "[anonymous user]";
                    }
                };
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
}
