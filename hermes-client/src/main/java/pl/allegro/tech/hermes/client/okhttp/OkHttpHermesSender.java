package pl.allegro.tech.hermes.client.okhttp;

import com.squareup.okhttp.*;
import pl.allegro.tech.hermes.client.HermesMessage;
import pl.allegro.tech.hermes.client.HermesResponse;
import pl.allegro.tech.hermes.client.HermesSender;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

import static pl.allegro.tech.hermes.client.HermesResponseBuilder.hermesResponse;

public class OkHttpHermesSender implements HermesSender {

    private final OkHttpClient client;

    public OkHttpHermesSender(OkHttpClient client) {
        this.client = client;
    }

    @Override
    public CompletableFuture<HermesResponse> send(URI uri, HermesMessage message) {
        CompletableFuture<HermesResponse> future = new CompletableFuture<>();

        RequestBody body = RequestBody.create(MediaType.parse(message.getContentType()), message.getBody());
        Request request = new Request.Builder()
                .addHeader(HermesSender.SCHEMA_VERSION_HEADER, Integer.toString(message.getSchemaVersion()))
                .post(body)
                .url(uri.toString())
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                future.completeExceptionally(e);
            }

            @Override
            public void onResponse(Response response) throws IOException {
                future.complete(fromOkHttpResponse(response));
            }
        });

        return future;
    }

    private HermesResponse fromOkHttpResponse(Response response) throws IOException {
        return hermesResponse()
                .withHeaderSupplier(response::header)
                .withHttpStatus(response.code())
                .withBody(response.body().string())
                .build();
    }
}
