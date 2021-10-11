package pl.allegro.tech.hermes.management.api;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.management.domain.console.ConsoleService;

import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Component
@Path("/")
@Api(value = "/", description = "Hermes console")
public class ConsoleEndpoint {

    private ConsoleService service;

    public ConsoleEndpoint(ConsoleService service) {
        this.service = service;
    }

    @GET
    @Path("/console")
    @Produces("application/javascript")
    @ApiOperation(value = "Hermes console configuration", httpMethod = HttpMethod.GET)
    public String getConfiguration() {
        return service.getConfiguration();
    }
}
