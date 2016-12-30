package pl.allegro.tech.hermes.management.api;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.Maintainer;
import pl.allegro.tech.hermes.management.domain.maintainer.MaintainerSourceNotFound;
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
    @Path("/sources/{source}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Lists maintainers from the given source matching the search string", response = List.class, httpMethod = HttpMethod.GET)
    public List<Maintainer> search(@PathParam("source") String source,
                                   @QueryParam("search") String searchString) {
        return maintainerSources.getByName(source)
                .map(s -> s.maintainersMatching(searchString))
                .orElseThrow(() -> new MaintainerSourceNotFound(source));
    }

    @GET
    @Path("/sources/{source}/{id}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Returns maintainer from the given source of the given id", response = List.class, httpMethod = HttpMethod.GET)
    public Maintainer get(@PathParam("source") String source,
                          @PathParam("id") String id) {
        return maintainerSources.getByName(source)
                .flatMap(s -> s.get(id)) // TODO different exception when source is found but id isn't
                .orElseThrow(() -> new MaintainerSourceNotFound(source));
    }

    @GET
    @Path("/sources")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Lists maintainer sources", response = List.class, httpMethod = HttpMethod.GET)
    public List<String> listSources() {
        return maintainerSources.names();
    }

}