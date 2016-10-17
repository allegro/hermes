package pl.allegro.tech.hermes.management.api.auth;

import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
@Priority(1999) // fixing equal values reordering issue of Jersey's 2.23.2 RankedComparator (Priorities.AUTHORIZATION=2000)
public class AuthorizationFilter implements ContainerRequestFilter {

    private final SecurityContextProvider securityContextProvider;

    @Autowired
    public AuthorizationFilter(SecurityContextProvider securityContextProvider) {
        this.securityContextProvider = securityContextProvider;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        requestContext.setSecurityContext(securityContextProvider.securityContext(requestContext));
    }
}