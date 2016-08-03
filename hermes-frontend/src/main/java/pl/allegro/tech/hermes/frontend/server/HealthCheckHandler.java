package pl.allegro.tech.hermes.frontend.server;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import pl.allegro.tech.hermes.frontend.services.HealthCheckService;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;

public class HealthCheckHandler implements HttpHandler {

    private final HealthCheckService healthCheckService;

    public HealthCheckHandler(HealthCheckService healthCheckService) {
        this.healthCheckService = healthCheckService;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (healthCheckService.isShutdown()) {
            unavailable(exchange);
        } else {
            success(exchange);
        }
    }

    private void success(HttpServerExchange exchange) {
        response(exchange, SC_OK, "UP");
    }

    private void unavailable(HttpServerExchange exchange) {
        response(exchange, SC_SERVICE_UNAVAILABLE, "SHUTDOWN");
    }

    private void response(HttpServerExchange exchange, int status, String data) {
        exchange.setStatusCode(status);
        exchange.getResponseSender().send(data);
        exchange.endExchange();
    }

}
