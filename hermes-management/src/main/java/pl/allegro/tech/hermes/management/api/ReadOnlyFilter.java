package pl.allegro.tech.hermes.management.api;

import org.glassfish.jersey.server.ContainerRequest;
import org.springframework.beans.factory.annotation.Autowired;
import pl.allegro.tech.hermes.management.api.auth.AuthorizationFilter;
import pl.allegro.tech.hermes.management.api.auth.Roles;
import pl.allegro.tech.hermes.management.domain.mode.ModeService;

import java.io.IOException;
import javax.annotation.Priority;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;

@Provider
@Priority(AuthorizationFilter.AUTHORIZATION_FILTER_PRIORITY + 2)
public class ReadOnlyFilter implements ContainerRequestFilter {
    private static final String READ_ONLY_ERROR_MESSAGE = "Action forbidden due to read-only mode";

    private final ModeService modeService;

    @Autowired
    public ReadOnlyFilter(ModeService modeService) {
        this.modeService = modeService;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (modeService.isReadOnlyEnabled()) {
            ContainerRequest req = (ContainerRequest) requestContext.getRequest();
            if (!isWhitelisted(req) && !isAdmin(requestContext)) {
                throw new ServiceUnavailableException(READ_ONLY_ERROR_MESSAGE);
            }
        }
    }

    private boolean isAdmin(ContainerRequestContext requestContext) {
        return requestContext.getSecurityContext().isUserInRole(Roles.ADMIN);
    }

    private boolean isWhitelisted(ContainerRequest req) {
        if (req.getMethod().equals("GET")) {
            return true;
        }
        String requestURI = req.getUriInfo().getPath();
        if (requestURI.startsWith("/query")) {
            return true;
        }
        if (requestURI.startsWith("/mode")) {
            return true;
        }
        if (requestURI.startsWith("/topics") && requestURI.endsWith("query")) {
            return true;
        }
        return false;
    }
}
