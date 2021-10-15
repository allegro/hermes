package pl.allegro.tech.hermes.management.api;

import org.glassfish.jersey.server.ContainerRequest;
import org.springframework.beans.factory.annotation.Autowired;
import pl.allegro.tech.hermes.management.domain.mode.ModeService;

import javax.annotation.Priority;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
@Priority(2001)
public class ReadOnlyFilter implements ContainerRequestFilter {
    private static final String READ_ONLY_ERROR_MESSAGE = "Action forbidden due to read-only mode";

    private final ModeService modeService;

    @Autowired
    public ReadOnlyFilter(ModeService modeService) {
        this.modeService = modeService;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (modeService.isReadOnlyEnabled() && !requestContext.getSecurityContext().isUserInRole("admin")) {
            ContainerRequest req = (ContainerRequest) requestContext.getRequest();
            if (!req.getMethod().equals("GET") && !isWhitelisted(req.getUriInfo().getPath())) {
                throw new ServiceUnavailableException(READ_ONLY_ERROR_MESSAGE);
            }
        }
    }

    private boolean isWhitelisted(String requestURI) {
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
