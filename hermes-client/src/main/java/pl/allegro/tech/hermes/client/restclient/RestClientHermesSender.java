package pl.allegro.tech.hermes.client.restclient;

import static java.util.stream.Collectors.toMap;
import static pl.allegro.tech.hermes.client.HermesResponseBuilder.hermesResponse;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.web.client.RestClient;
import pl.allegro.tech.hermes.client.HermesMessage;
import pl.allegro.tech.hermes.client.HermesResponse;
import pl.allegro.tech.hermes.client.HermesSender;

/**
 * {@link HermesSender} implementation based on Spring {@link RestClient}.
 *
 * <p>{@link RestClient} performs blocking HTTP calls, so this sender offloads request execution to
 * a dedicated {@link ExecutorService} and returns a {@link CompletableFuture} representing that
 * work.
 *
 * <p>Instances created with {@link #RestClientHermesSender(RestClient)} create and own a default
 * executor service, which should be released with {@link #close()}. Instances created with {@link
 * #RestClientHermesSender(RestClient, ExecutorService)} use the provided executor service without
 * taking ownership of its lifecycle.
 */
public class RestClientHermesSender implements HermesSender, AutoCloseable {

  private final RestClient restClient;
  private final ExecutorService executorService;
  private final boolean ownsExecutorService;

  public RestClientHermesSender(RestClient restClient) {
    this(restClient, defaultExecutorService(), true);
  }

  public RestClientHermesSender(RestClient restClient, ExecutorService executorService) {
    this(restClient, executorService, false);
  }

  private RestClientHermesSender(
      RestClient restClient, ExecutorService executorService, boolean ownsExecutorService) {
    this.restClient = restClient;
    this.executorService = executorService;
    this.ownsExecutorService = ownsExecutorService;
  }

  private static ExecutorService defaultExecutorService() {
    int poolSize = Runtime.getRuntime().availableProcessors() * 2;
    AtomicInteger counter = new AtomicInteger(0);
    return new ThreadPoolExecutor(
        poolSize,
        poolSize,
        60L,
        TimeUnit.SECONDS,
        new SynchronousQueue<>(),
        runnable -> new Thread(runnable, "hermes-rest-client-sender-" + counter.getAndIncrement()),
        new ThreadPoolExecutor.CallerRunsPolicy());
  }

  @Override
  public CompletableFuture<HermesResponse> send(URI uri, HermesMessage message) {
    return CompletableFuture.supplyAsync(
        () ->
            restClient
                .post()
                .uri(uri)
                .headers(httpHeaders -> httpHeaders.setAll(message.getHeaders()))
                .body(message.getBody())
                .exchange(
                    (request, response) -> {
                      if (response.getStatusCode().is2xxSuccessful()) {
                        return hermesResponse(message)
                            .withBody(
                                new String(
                                    response.getBody().readAllBytes(), StandardCharsets.UTF_8))
                            .withHttpStatus(response.getStatusCode().value())
                            .withHeaderSupplier(
                                header ->
                                    convertToCaseInsensitiveMap(
                                            response.getHeaders().toSingleValueMap())
                                        .get(header))
                            .build();
                      } else {
                        return hermesResponse(message)
                            .withBody("")
                            .withHttpStatus(response.getStatusCode().value())
                            .withHeaderSupplier(
                                header ->
                                    convertToCaseInsensitiveMap(
                                            response.getHeaders().toSingleValueMap())
                                        .get(header))
                            .build();
                      }
                    }),
        executorService);
  }

  @Override
  public void close() {
    if (ownsExecutorService) {
      executorService.shutdown();
    }
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
