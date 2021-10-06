package pl.allegro.tech.hermes.management.api;

import org.glassfish.jersey.server.mvc.Viewable;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/")
public class UiResource {
    @GET
    public Viewable getIndex() {
        return new Viewable("/index.html");
    }
}