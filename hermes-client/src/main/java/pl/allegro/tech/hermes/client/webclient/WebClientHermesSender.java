package pl.allegro.tech.hermes.client.webclient;

import static java.util.stream.Collectors.toMap;
import static pl.allegro.tech.hermes.client.HermesResponseBuilder.hermesResponse;

import java.net.URI;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import org.springframework.web.reactive.function.client.WebClient;
import pl.allegro.tech.hermes.client.HermesMessage;
import pl.allegro.tech.hermes.client.HermesResponse;
import pl.allegro.tech.hermes.client.HermesSender;
import pl.allegro.tech.hermes.client.ReactiveHermesSender;
import reactor.core.publisher.Mono;

public class WebClientHermesSender implements HermesSender, ReactiveHermesSender {

  private static final Mono<String> NO_BODY = Mono.just("");
  private final WebClient webClient;

  public WebClientHermesSender(WebClient webClient) {
    this.webClient = webClient;
  }

  @Override
  public Mono<HermesResponse> sendReactively(URI uri, HermesMessage message) {
    return webClient
        .post()
        .uri(uri)
        .syncBody(message.getBody())
        .headers(httpHeaders -> httpHeaders.setAll(message.getHeaders()))
        .exchange()
        .flatMap(
            response ->
                response
                    .bodyToMono(String.class)
                    .switchIfEmpty(NO_BODY)
                    .map(
                        body ->
                            hermesResponse(message)
                                .withBody(body)
                                .withHttpStatus(response.statusCode().value())
                                .withHeaderSupplier(
                                    header ->
                                        convertToCaseInsensitiveMap(
                                                response
                                                    .headers()
                                                    .asHttpHeaders()
                                                    .toSingleValueMap())
                                            .get(header))
                                .build()));
  }

  @Override
  public CompletableFuture<HermesResponse> send(URI uri, HermesMessage message) {
    return sendReactively(uri, message).toFuture();
  }

  private TreeMap<String, String> convertToCaseInsensitiveMap(Map<String, String> hashMap) {
    return hashMap.entrySet().stream()
        .collect(
            toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (oldVal, newVal) -> newVal,
                () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER)));
  }
}
