package pl.allegro.tech.hermes.management.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.management.domain.console.ConsoleService;

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
  @Deprecated
  public String getConfiguration() {
    return service.getConfiguration();
  }

  @GET
  @Path("/console")
  @Produces("application/json")
  @ApiOperation(value = "Hermes console configuration", httpMethod = HttpMethod.GET)
  public String getConfigurationJson() {
    return service.getConfigurationJson();
  }
}
