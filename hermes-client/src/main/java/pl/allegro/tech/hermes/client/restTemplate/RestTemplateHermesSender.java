package pl.allegro.tech.hermes.client.restTemplate; // CHECKSTYLE.SUPPRESS: PackageName

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
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

import static java.util.stream.Collectors.toMap;
import static pl.allegro.tech.hermes.client.HermesResponseBuilder.hermesResponse;

/**
 * RestTemplateHermesSender.
 *
 * @deprecated as of Hermes 2.2.10, in favor of {@link pl.allegro.tech.hermes.client.webclient.WebClientHermesSender}
 */
@Deprecated
public class RestTemplateHermesSender implements HermesSender {

    private final AsyncRestTemplate template;

    public RestTemplateHermesSender(AsyncRestTemplate template) {
        this.template = template;
    }

    @Override
    public CompletableFuture<HermesResponse> send(URI uri, HermesMessage message) {
        CompletableFuture<HermesResponse> future = new CompletableFuture<>();
        template.postForEntity(uri, new HttpEntity<>(message.getBody(), new LinkedMultiValueMap<String, String>() {
                    {
                        message.consumeHeaders(this::add);
                    }
                }), String.class)
                .addCallback(new ListenableFutureCallback<ResponseEntity<?>>() {
                    @Override
                    public void onSuccess(ResponseEntity response) {
                        future.complete(fromRestTemplateResponse(message, response));
                    }

                    @Override
                    public void onFailure(Throwable exception) {
                        if (exception instanceof HttpStatusCodeException) {
                            future.complete(fromHttpStatusCodeException(message, (HttpStatusCodeException) exception));
                        } else {
                            future.completeExceptionally(exception);
                        }
                    }
                });
        return future;
    }

    private HermesResponse fromRestTemplateResponse(HermesMessage message, ResponseEntity<?> response) {
        return hermesResponse(message)
                .withHttpStatus(response.getStatusCode().value())
                .withBody(response.toString())
                .withHeaderSupplier(header -> convertToCaseInsensitiveMap(response.getHeaders().toSingleValueMap())
                        .getOrDefault(header, null))
                .build();
    }

    private HermesResponse fromHttpStatusCodeException(HermesMessage message, HttpStatusCodeException exception) {
        return hermesResponse(message)
                .withHttpStatus(exception.getStatusCode().value())
                .withBody(exception.getResponseBodyAsString())
                .build();
    }

    private TreeMap<String, String> convertToCaseInsensitiveMap(Map<String, String> hashMap) {
        return hashMap.entrySet().stream()
                .collect(toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldVal, newVal) -> newVal,
                        () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER)
                ));
    }
}
