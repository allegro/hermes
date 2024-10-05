package pl.allegro.tech.hermes.client.jersey;

import static pl.allegro.tech.hermes.client.HermesResponseBuilder.hermesResponse;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.InvocationCallback;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import pl.allegro.tech.hermes.client.HermesMessage;
import pl.allegro.tech.hermes.client.HermesResponse;
import pl.allegro.tech.hermes.client.HermesSender;

public class JerseyHermesSender implements HermesSender {
  private final Client client;

  public JerseyHermesSender(Client client) {
    this.client = client;
  }

  @Override
  public CompletableFuture<HermesResponse> send(URI uri, HermesMessage message) {
    CompletableFuture<HermesResponse> future = new CompletableFuture<>();
    Invocation.Builder builder = client.target(uri).request();
    message.consumeHeaders(builder::header);
    builder
        .async()
        .post(
            Entity.entity(message.getBody(), message.getContentType()),
            new InvocationCallback<Response>() {
              @Override
              public void completed(Response response) {
                future.complete(fromJerseyResponse(message, response));
              }

              @Override
              public void failed(Throwable exception) {
                future.completeExceptionally(exception);
              }
            });
    return future;
  }

  private HermesResponse fromJerseyResponse(HermesMessage message, Response response) {
    return hermesResponse(message)
        .withHttpStatus(response.getStatus())
        .withBody(response.readEntity(String.class))
        .withHeaderSupplier(response::getHeaderString)
        .build();
  }
}
