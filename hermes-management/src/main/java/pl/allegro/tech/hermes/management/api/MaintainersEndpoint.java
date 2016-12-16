package pl.allegro.tech.hermes.management.api;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.management.domain.maintainer.MaintainerSources;

import javax.ws.rs.*;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("/maintainers")
@Api(value = "/maintainers", description = "Provides maintainers information")
public class MaintainersEndpoint {

    private MaintainerSources maintainerSources;

    @Autowired
    public MaintainersEndpoint(MaintainerSources maintainerSources) {
        this.maintainerSources = maintainerSources;
    }

    @GET
    @Path("/sources/{source}/{searchString}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Lists maintainers from the given source matching the search string", response = List.class, httpMethod = HttpMethod.GET)
    public List<String> get(@PathParam("source") String source, @PathParam("searchString") String searchString) {
        return maintainerSources.getByName(source).maintainersMatching(searchString);
    }

    @GET
    @Path("/sources")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Lists maintainer sources", response = List.class, httpMethod = HttpMethod.GET)
    public List<String> get() {
        return maintainerSources.names();
    }

}