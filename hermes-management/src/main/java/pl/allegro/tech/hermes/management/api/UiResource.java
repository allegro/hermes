package pl.allegro.tech.hermes.management.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.glassfish.jersey.server.mvc.Viewable;

import static jakarta.ws.rs.core.MediaType.TEXT_HTML;

public class UiResource {
    @GET
    @Produces(TEXT_HTML)
    @Path("/")
    public Viewable getIndex() {
        return new Viewable("/index.html");
    }

    @GET
    @Produces(TEXT_HTML)
    @Path("/ui")
    public Viewable getIndexUi() {
        return new Viewable("/index.html");
    }
}
