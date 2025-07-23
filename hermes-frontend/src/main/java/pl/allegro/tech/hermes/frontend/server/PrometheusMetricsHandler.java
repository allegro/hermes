package pl.allegro.tech.hermes.frontend.server;

import static io.undertow.util.StatusCodes.OK;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;

public class PrometheusMetricsHandler implements HttpHandler {

  private final PrometheusMeterRegistry meterRegistry;
  private static final HttpString httpString = new HttpString("Content-Type");

  public PrometheusMetricsHandler(PrometheusMeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) {
    exchange.getResponseHeaders().add(httpString, "text/plain;version=0.0.4;charset=utf-8");
    response(exchange, OK, meterRegistry.scrape());
  }

  private void response(HttpServerExchange exchange, int status, String data) {
    exchange.setStatusCode(status);
    exchange.getResponseSender().send(data);
  }
}
