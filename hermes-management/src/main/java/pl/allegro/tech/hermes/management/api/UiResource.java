package pl.allegro.tech.hermes.management.api;

import static jakarta.ws.rs.core.MediaType.TEXT_HTML;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.glassfish.jersey.server.mvc.Viewable;

@Path("/")
public class UiResource {
  @GET
  @Produces(TEXT_HTML)
  public Viewable getIndex() {
    return new Viewable("/index.html");
  }
}
