package pl.allegro.tech.hermes.management.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.glassfish.jersey.server.mvc.Viewable;

import static jakarta.ws.rs.core.MediaType.TEXT_HTML;

@Path("/ui")
public class UiResourceV2 {
    @GET
    @Produces(TEXT_HTML)
    public Viewable getIndexV2() {
        return new Viewable("/index.html");
    }
}
