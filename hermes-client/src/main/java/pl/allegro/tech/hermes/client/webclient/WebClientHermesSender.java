package pl.allegro.tech.hermes.client.webclient;

import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import pl.allegro.tech.hermes.client.HermesMessage;
import pl.allegro.tech.hermes.client.HermesResponse;
import pl.allegro.tech.hermes.client.HermesSender;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;
import static pl.allegro.tech.hermes.client.HermesResponseBuilder.hermesResponse;

public class WebClientHermesSender implements HermesSender {

    private final WebClient webClient;

    public WebClientHermesSender(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public CompletableFuture<HermesResponse> send(URI uri, HermesMessage message) {
        final Mono<ClientResponse> clientResponse = webClient.post()
                .uri(uri)
                .syncBody(message.getBody())
                .headers(httpHeaders -> httpHeaders.setAll(message.getHeaders()))
                .exchange();

        return clientResponse
                .toFuture()
                .thenApply(response -> response.bodyToMono(String.class).toFuture())
                .thenCompose(Function.identity())
                .thenCombine(
                        clientResponse.toFuture().thenApply(res ->
                                new HttpData(res.rawStatusCode(), res.headers().asHttpHeaders())),
                        (body, httpData) ->
                                hermesResponse()
                                        .withBody(body)
                                        .withHttpStatus(httpData.getStatusCode())
                                        .withHeaderSupplier(header -> convertToCaseInsensitiveMap(httpData.getHeaders())
                                                .getOrDefault(header, null))
                                .build());
    }

    private static class HttpData {
        private final int statusCode;
        private final HttpHeaders httpHeaders;

        HttpData(int statusCode, HttpHeaders httpHeaders) {
            this.statusCode = statusCode;
            this.httpHeaders = httpHeaders;
        }

        int getStatusCode() {
            return statusCode;
        }

        Map<String, String> getHeaders() {
            return httpHeaders.toSingleValueMap();
        }
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
