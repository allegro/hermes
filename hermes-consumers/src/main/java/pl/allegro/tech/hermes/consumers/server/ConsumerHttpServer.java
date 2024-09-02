package pl.allegro.tech.hermes.consumers.server;

import static jakarta.ws.rs.core.Response.Status.OK;
import static pl.allegro.tech.hermes.consumers.health.Checks.SUBSCRIPTIONS;
import static pl.allegro.tech.hermes.consumers.health.Checks.SUBSCRIPTIONS_COUNT;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import pl.allegro.tech.hermes.consumers.health.ConsumerMonitor;

public class ConsumerHttpServer {

  private final HttpServer server;

  private static final String STATUS_UP = "{\"status\": \"UP\"}";

  public ConsumerHttpServer(
      int healthCheckPort,
      ConsumerMonitor monitor,
      ObjectMapper mapper,
      PrometheusMeterRegistry meterRegistry)
      throws IOException {
    server = createServer(healthCheckPort);
    server.createContext("/status/health", (exchange) -> respondWithJson(exchange, STATUS_UP));
    server.createContext(
        "/status/subscriptions",
        (exchange) -> respondWithObject(exchange, mapper, monitor.check(SUBSCRIPTIONS)));
    server.createContext(
        "/status/subscriptionsCount",
        (exchange) -> respondWithObject(exchange, mapper, monitor.check(SUBSCRIPTIONS_COUNT)));
    server.createContext(
        "/status/prometheus", (exchange) -> respondWithString(exchange, meterRegistry.scrape()));
  }

  private HttpServer createServer(int port) throws IOException {
    HttpServer httpServer = HttpServer.create(new InetSocketAddress(port), 0);
    httpServer.setExecutor(null);
    return httpServer;
  }

  private static void respondWithObject(
      HttpExchange httpExchange, ObjectMapper mapper, Object response) throws IOException {
    respondWithJson(httpExchange, mapper.writeValueAsString(response));
  }

  private static void respondWithJson(HttpExchange httpExchange, String response)
      throws IOException {
    httpExchange.getResponseHeaders().put("Content-Type", List.of("application/json"));
    respondWithString(httpExchange, response);
  }

  private static void respondWithString(HttpExchange httpExchange, String response)
      throws IOException {
    httpExchange.sendResponseHeaders(OK.getStatusCode(), response.length());
    OutputStream os = httpExchange.getResponseBody();
    os.write(response.getBytes());
    os.close();
  }

  public void start() {
    server.start();
  }

  public void stop() {
    server.stop(0);
  }

  public int getPort() {
    return server.getAddress().getPort();
  }
}
