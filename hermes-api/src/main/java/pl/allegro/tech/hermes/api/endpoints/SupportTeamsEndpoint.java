package pl.allegro.tech.hermes.api.endpoints;

import pl.allegro.tech.hermes.api.SupportTeam;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("supportTeams")
public interface SupportTeamsEndpoint {

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/{searchString}")
    List<SupportTeam> get(@PathParam("searchString") String searchString);

    @GET
    @Produces(APPLICATION_JSON)
    @Path("/{searchString}")
    Response getAsResponse(@PathParam("searchString") String searchString);
}
