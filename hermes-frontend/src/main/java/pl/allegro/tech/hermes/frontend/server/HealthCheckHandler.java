package pl.allegro.tech.hermes.frontend.server;

import static io.undertow.util.StatusCodes.OK;
import static io.undertow.util.StatusCodes.SERVICE_UNAVAILABLE;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import pl.allegro.tech.hermes.frontend.readiness.HealthCheckService;

public class HealthCheckHandler implements HttpHandler {

  private final HealthCheckService healthCheckService;

  public HealthCheckHandler(HealthCheckService healthCheckService) {
    this.healthCheckService = healthCheckService;
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) {
    if (healthCheckService.isShutdown()) {
      unavailable(exchange);
    } else {
      success(exchange);
    }
  }

  private void success(HttpServerExchange exchange) {
    response(exchange, OK, "UP");
  }

  private void unavailable(HttpServerExchange exchange) {
    response(exchange, SERVICE_UNAVAILABLE, "SHUTDOWN");
  }

  private void response(HttpServerExchange exchange, int status, String data) {
    exchange.setStatusCode(status);
    exchange.getResponseSender().send(data);
  }
}
