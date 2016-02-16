package pl.allegro.tech.hermes.client.restTemplate;

import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.HttpStatusCodeException;
import pl.allegro.tech.hermes.client.HermesMessage;
import pl.allegro.tech.hermes.client.HermesResponse;
import pl.allegro.tech.hermes.client.HermesSender;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static pl.allegro.tech.hermes.client.HermesResponseBuilder.hermesResponse;

public class RestTemplateHermesSender implements HermesSender {

    private final AsyncRestTemplate template;

    public RestTemplateHermesSender(AsyncRestTemplate template) {
        this.template = template;
    }

    @Override
    public CompletableFuture<HermesResponse> send(URI uri, HermesMessage message) {
        CompletableFuture<HermesResponse> future = new CompletableFuture<>();
        template.postForEntity(uri, new HttpEntity<>(message.getBody(), new LinkedMultiValueMap<String, String>() {{
            add(CONTENT_TYPE, message.getContentType());
            add(SCHEMA_VERSION_HEADER, Integer.toString(message.getSchemaVersion()));
        }}), String.class)
                .addCallback(new ListenableFutureCallback<ResponseEntity>() {
                    @Override
                    public void onSuccess(ResponseEntity response) {
                        future.complete(fromRestTemplateResponse(response));
                    }

                    @Override
                    public void onFailure(Throwable exception) {
                        if (exception instanceof HttpStatusCodeException) {
                            future.complete(fromHttpStatusCodeException((HttpStatusCodeException) exception));
                        } else {
                            future.completeExceptionally(exception);
                        }
                    }
                });
        return future;
    }

    private HermesResponse fromRestTemplateResponse(ResponseEntity response) {
        return hermesResponse()
                .withHttpStatus(response.getStatusCode().value())
                .withBody(response.toString())
                .withHeaderSupplier(header -> response.getHeaders().toSingleValueMap().getOrDefault(header, null))
                .build();
    }

    private HermesResponse fromHttpStatusCodeException(HttpStatusCodeException exception) {
        return hermesResponse()
                .withHttpStatus(exception.getStatusCode().value())
                .withBody(exception.getResponseBodyAsString())
                .build();
    }
}
