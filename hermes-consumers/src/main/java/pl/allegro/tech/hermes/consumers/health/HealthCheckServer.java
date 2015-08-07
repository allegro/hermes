package pl.allegro.tech.hermes.consumers.health;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;

import javax.inject.Inject;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import static javax.ws.rs.core.Response.Status.OK;

public class HealthCheckServer {

    private final HttpServer server;

    @Inject
    public HealthCheckServer(ConfigFactory configFactory) throws IOException {
        server = createServer(configFactory.getIntProperty(Configs.CONSUMER_HEALTH_CHECK_PORT));
        server.createContext("/status/health", new HealthCheckHandler());
    }

    private HttpServer createServer(int port) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        httpServer.setExecutor(null);
        return httpServer;
    }

    private static class HealthCheckHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            String response = "{\"status\": \"UP\"}";
            httpExchange.sendResponseHeaders(OK.getStatusCode(), response.length());
            OutputStream os = httpExchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }
}
