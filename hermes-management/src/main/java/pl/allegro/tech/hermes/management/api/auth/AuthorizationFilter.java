package pl.allegro.tech.hermes.management.api.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

@Component
@Provider
@Priority(AuthorizationFilter.AUTHORIZATION_FILTER_PRIORITY)
public class AuthorizationFilter implements ContainerRequestFilter {

    public static final String OWNERSHIP_RESOLVER = "ownership-resolver";
    public static final int AUTHORIZATION_FILTER_PRIORITY = 1999; // fixing equal values reordering issue of Jersey's 2.23.2 RankedComparator (Priorities.AUTHORIZATION=2000)

    private final SecurityProvider securityProvider;

    @Autowired
    public AuthorizationFilter(SecurityProvider securityProvider) {
        this.securityProvider = securityProvider;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        SecurityProvider.HermesSecurity security = securityProvider.security(requestContext);
        requestContext.setSecurityContext(security.getSecurityContext());
        requestContext.setProperty(OWNERSHIP_RESOLVER, security.getOwnershipResolver());
    }
}