package pl.allegro.tech.hermes.management.api;

import org.glassfish.jersey.server.mvc.Viewable;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import static javax.ws.rs.core.MediaType.TEXT_HTML;

@Path("/")
public class UiResource {
    @GET
    @Produces(TEXT_HTML)
    public Viewable getIndex() {
        return new Viewable("/index.html");
    }
}