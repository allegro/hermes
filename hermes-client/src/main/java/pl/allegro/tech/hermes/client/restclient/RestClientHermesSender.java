package pl.allegro.tech.hermes.client.restclient;

import org.springframework.web.client.RestClient;
import pl.allegro.tech.hermes.client.HermesMessage;
import pl.allegro.tech.hermes.client.HermesResponse;
import pl.allegro.tech.hermes.client.HermesSender;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

import static java.util.stream.Collectors.toMap;
import static pl.allegro.tech.hermes.client.HermesResponseBuilder.hermesResponse;


public class RestClientHermesSender implements HermesSender {

    private final RestClient restClient;


    public RestClientHermesSender(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public CompletableFuture<HermesResponse> send(URI uri, HermesMessage message) {
        CompletableFuture<HermesResponse> future = new CompletableFuture<>();

        var received = restClient
                .post()
                .uri(uri)
                .headers(httpHeaders -> httpHeaders.setAll(message.getHeaders()))
                .body(message.getBody())
                .exchange((request, response) -> {
                    if (response.getStatusCode().is2xxSuccessful()) {
                        return hermesResponse(message)
                                .withBody(new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8))
                                .withHttpStatus(response.getStatusCode().value())
                                .withHeaderSupplier(header -> convertToCaseInsensitiveMap(response.getHeaders().toSingleValueMap()).get(header))
                                .build();
                    } else {
                        return hermesResponse(message)
                                .withBody("")
                                .withHttpStatus(response.getStatusCode().value())
                                .withHeaderSupplier(header -> convertToCaseInsensitiveMap(response.getHeaders().toSingleValueMap()).get(header))
                                .build();
                    }
                });

        future.complete(received);
        return future;
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
