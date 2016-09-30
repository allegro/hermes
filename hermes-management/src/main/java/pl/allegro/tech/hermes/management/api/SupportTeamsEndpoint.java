package pl.allegro.tech.hermes.management.api;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.SupportTeam;
import pl.allegro.tech.hermes.management.domain.supportTeam.SupportTeamCache;

import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("/supportTeams")
@Api(value = "/supportTeams", description = "Provides basic support team information")
public class SupportTeamsEndpoint {

    private SupportTeamCache supportTeamCache;

    @Autowired
    public SupportTeamsEndpoint(SupportTeamCache supportTeamCache) {
        this.supportTeamCache = supportTeamCache;
    }

    @GET
    @Path("/{searchString}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Lists support teams matching group name", response = List.class, httpMethod = HttpMethod.GET)
    public List<SupportTeam> get(@PathParam("searchString") String searchString) {
        return supportTeamCache.getSupportTeams(searchString);
    }
}