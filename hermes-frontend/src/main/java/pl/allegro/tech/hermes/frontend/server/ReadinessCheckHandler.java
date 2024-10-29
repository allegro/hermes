package pl.allegro.tech.hermes.frontend.server;

import static io.undertow.util.StatusCodes.OK;
import static io.undertow.util.StatusCodes.SERVICE_UNAVAILABLE;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import pl.allegro.tech.hermes.frontend.readiness.HealthCheckService;
import pl.allegro.tech.hermes.frontend.readiness.ReadinessChecker;

public class ReadinessCheckHandler implements HttpHandler {

  private final ReadinessChecker readinessChecker;
  private final HealthCheckService healthCheckService;

  public ReadinessCheckHandler(
      ReadinessChecker readinessChecker, HealthCheckService healthCheckService) {
    this.readinessChecker = readinessChecker;
    this.healthCheckService = healthCheckService;
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) {
    if (!healthCheckService.isShutdown() && readinessChecker.isReady()) {
      success(exchange);
    } else {
      unavailable(exchange);
    }
  }

  private void success(HttpServerExchange exchange) {
    response(exchange, OK, "READY");
  }

  private void unavailable(HttpServerExchange exchange) {
    response(exchange, SERVICE_UNAVAILABLE, "NOT_READY");
  }

  private void response(HttpServerExchange exchange, int status, String data) {
    exchange.setStatusCode(status);
    exchange.getResponseSender().send(data);
  }
}
