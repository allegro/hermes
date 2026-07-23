package pl.allegro.tech.hermes.frontend.server;

import static io.undertow.util.StatusCodes.SERVICE_UNAVAILABLE;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import pl.allegro.tech.hermes.frontend.readiness.HealthCheckService;
import pl.allegro.tech.hermes.frontend.readiness.ReadinessChecker;

public class PublishingReadinessHandler implements HttpHandler {

  private final HttpHandler next;
  private final ReadinessChecker readinessChecker;
  private final HealthCheckService healthCheckService;

  public PublishingReadinessHandler(
      HttpHandler next, ReadinessChecker readinessChecker, HealthCheckService healthCheckService) {
    this.next = next;
    this.readinessChecker = readinessChecker;
    this.healthCheckService = healthCheckService;
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception {
    if (!healthCheckService.isShutdown() && readinessChecker.isReady()) {
      next.handleRequest(exchange);
    } else {
      exchange.setStatusCode(SERVICE_UNAVAILABLE);
      exchange.getResponseSender().send("NOT_READY");
    }
  }
}
