package pl.allegro.tech.hermes.client.restTemplate;

import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.HttpStatusCodeException;
import pl.allegro.tech.hermes.client.HermesMessage;
import pl.allegro.tech.hermes.client.HermesResponse;
import pl.allegro.tech.hermes.client.HermesSender;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class RestTemplateHermesSender implements HermesSender {
    private final AsyncRestTemplate template;
    private final MultiValueMap<String, String> headers = new LinkedMultiValueMap<String, String>() {{
        add(CONTENT_TYPE, APPLICATION_JSON_VALUE);
    }};

    public RestTemplateHermesSender(AsyncRestTemplate template) {
        this.template = template;
    }

    @Override
    public CompletableFuture<HermesResponse> send(URI uri, HermesMessage message) {
        CompletableFuture<HermesResponse> future = new CompletableFuture<>();
        template.postForEntity(uri, new HttpEntity<>(message.getBody(), headers), String.class)
                .addCallback(new ListenableFutureCallback<ResponseEntity>() {
                    @Override
                    public void onSuccess(ResponseEntity result) {
                        future.complete(new RestTemplateHermesResponse(result));
                    }
                    @Override
                    public void onFailure(Throwable ex) {
                        if (ex instanceof HttpStatusCodeException) {
                            future.complete(getResponseForException((HttpStatusCodeException) ex));
                        } else {
                            future.completeExceptionally(ex);
                        }
                    }
                });
        return future;
    }

    private HermesResponse getResponseForException(HttpStatusCodeException ex) {
        return new HermesResponse() {
            @Override
            public int getHttpStatus() {
                return ex.getStatusCode().value();
            }

            @Override
            public String getBody() {
                return ex.getResponseBodyAsString();
            }
        };
    }
}
