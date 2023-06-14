package pl.allegro.tech.hermes.management.api;

import org.glassfish.jersey.server.mvc.Viewable;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import static jakarta.ws.rs.core.MediaType.TEXT_HTML;

@Path("/")
public class UiResource {
    @GET
    @Produces(TEXT_HTML)
    public Viewable getIndex() {
        return new Viewable("/index.html");
    }
}