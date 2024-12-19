package pl.allegro.tech.hermes.management.api;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import io.swagger.annotations.ApiOperation;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import java.util.List;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.management.api.auth.Roles;
import pl.allegro.tech.hermes.management.domain.detection.InactiveTopic;
import pl.allegro.tech.hermes.management.domain.detection.InactiveTopicsStorageService;

@Component
@Path("/inactive-topics")
public class InactiveTopicsEndpoint {
  private final InactiveTopicsStorageService storageService;

  public InactiveTopicsEndpoint(InactiveTopicsStorageService storageService) {
    this.storageService = storageService;
  }

  @GET
  @Produces(APPLICATION_JSON)
  @RolesAllowed(Roles.ADMIN)
  @ApiOperation(value = "All inactive topics", response = List.class, httpMethod = HttpMethod.GET)
  public List<InactiveTopic> getConsumersWorkloadConstraints() {
    return storageService.getInactiveTopics();
  }
}
