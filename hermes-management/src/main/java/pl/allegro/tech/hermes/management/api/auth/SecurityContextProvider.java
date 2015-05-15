package pl.allegro.tech.hermes.management.api.auth;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;

public interface SecurityContextProvider {
    
    SecurityContext securityContext(ContainerRequestContext requestContext);
    
}
