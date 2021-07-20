package pl.allegro.tech.hermes.frontend.server;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import pl.allegro.tech.hermes.frontend.services.ReadinessCheckService;

import static io.undertow.util.StatusCodes.OK;
import static io.undertow.util.StatusCodes.SERVICE_UNAVAILABLE;

public class ReadinessCheckHandler implements HttpHandler {

    private final ReadinessCheckService readinessCheckService;

    public ReadinessCheckHandler(ReadinessCheckService readinessCheckService) {
        this.readinessCheckService = readinessCheckService;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (readinessCheckService.isReady()) {
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
