package pl.allegro.tech.hermes.frontend.server;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import static io.undertow.util.StatusCodes.OK;

public class PrometheusMetricsHandler implements HttpHandler {

    private final PrometheusMeterRegistry meterRegistry;

    public PrometheusMetricsHandler(PrometheusMeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        response(exchange, OK, meterRegistry.scrape());
    }

    private void response(HttpServerExchange exchange, int status, String data) {
        exchange.setStatusCode(status);
        exchange.getResponseSender().send(data);
    }

}
