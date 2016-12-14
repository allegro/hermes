package pl.allegro.tech.hermes.management.api;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.management.domain.maintainer.MaintainerSource;

import javax.ws.rs.*;
import java.util.List;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@Path("/maintainers")
@Api(value = "/maintainers", description = "Provides basic maintainers information")
public class MaintainersEndpoint {

    private MaintainerSource maintainerSource;

    @Autowired
    public MaintainersEndpoint(MaintainerSource maintainerSource) {
        this.maintainerSource = maintainerSource;
    }

    @GET
    @Path("/{source}/{searchString}")
    @Produces(APPLICATION_JSON)
    @ApiOperation(value = "Lists maintainers matching search string", response = List.class, httpMethod = HttpMethod.GET)
    public List<String> get(@PathParam("source") String source, @PathParam("searchString") String searchString) {
        return maintainerSource.maintainersMatching(searchString);
    }
}