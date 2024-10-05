package pl.allegro.tech.hermes.management.api;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.ErrorCode;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.management.domain.ManagementException;
import pl.allegro.tech.hermes.management.domain.clients.IframeSource;
import pl.allegro.tech.hermes.management.domain.clients.OfflineClientsService;

@Component
@Path("/topics")
public class OfflineClientsEndpoint {

  private static final Logger logger = LoggerFactory.getLogger(OfflineClientsEndpoint.class);

  private final Optional<OfflineClientsService> offlineClientsService;

  OfflineClientsEndpoint(Optional<OfflineClientsService> offlineClientsService) {
    if (!offlineClientsService.isPresent()) {
      logger.info("Offline clients bean is absent");
    }
    this.offlineClientsService = offlineClientsService;
  }

  @GET
  @Path("/{topic}/offline-clients-source")
  @Produces(APPLICATION_JSON)
  public Response find(@PathParam("topic") String topic) {
    return offlineClientsService
        .map(
            service ->
                Response.ok(
                        new IframeSource(
                            service.getIframeSource(TopicName.fromQualifiedName(topic))))
                    .build())
        .orElseThrow(OfflineClientsServiceAbsentException::new);
  }

  private static class OfflineClientsServiceAbsentException extends ManagementException {

    OfflineClientsServiceAbsentException() {
      super("Offline clients implementation is absent");
    }

    @Override
    public ErrorCode getCode() {
      return ErrorCode.IMPLEMENTATION_ABSENT;
    }
  }
}
