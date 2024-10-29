package pl.allegro.tech.hermes.client.okhttp;

import static pl.allegro.tech.hermes.client.HermesResponseBuilder.hermesResponse;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import pl.allegro.tech.hermes.client.HermesMessage;
import pl.allegro.tech.hermes.client.HermesResponse;
import pl.allegro.tech.hermes.client.HermesSender;

public class OkHttpHermesSender implements HermesSender {

  private final OkHttpClient client;

  public OkHttpHermesSender(OkHttpClient client) {
    this.client = client;
  }

  @Override
  public CompletableFuture<HermesResponse> send(URI uri, HermesMessage message) {
    CompletableFuture<HermesResponse> future = new CompletableFuture<>();

    RequestBody body =
        RequestBody.create(MediaType.parse(message.getContentType()), message.getBody());
    Request.Builder builder = new Request.Builder();
    message.consumeHeaders(builder::addHeader);
    Request request = builder.post(body).url(uri.toString()).build();

    client
        .newCall(request)
        .enqueue(
            new Callback() {
              @Override
              public void onFailure(Call call, IOException e) {
                future.completeExceptionally(e);
              }

              @Override
              public void onResponse(Call call, Response response) throws IOException {
                future.complete(fromOkHttpResponse(message, response));
              }
            });

    return future;
  }

  HermesResponse fromOkHttpResponse(HermesMessage message, Response response) throws IOException {
    return hermesResponse(message)
        .withHeaderSupplier(response::header)
        .withHttpStatus(response.code())
        .withBody(response.body().string())
        .withProtocol(response.protocol().toString())
        .build();
  }
}
