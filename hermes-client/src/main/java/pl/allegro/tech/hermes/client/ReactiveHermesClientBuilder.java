package pl.allegro.tech.hermes.client;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.function.Supplier;
import pl.allegro.tech.hermes.client.metrics.MetricsMessageDeliveryListener;
import pl.allegro.tech.hermes.client.metrics.MetricsProvider;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

public class ReactiveHermesClientBuilder {

  private final ReactiveHermesSender sender;
  private URI uri = URI.create("http://localhost:8080");
  private final Map<String, String> defaultHeaders = new HashMap<>();
  private int retries = 3;
  private Predicate<HermesResponse> retryCondition = new HermesClientBasicRetryCondition();
  private long retrySleepInMillis = 100;
  private long maxRetrySleepInMillis = 300;
  private double jitterFactor = 0.5d;
  private Supplier<Scheduler> schedulerFactory =
      () -> Schedulers.fromExecutor(Executors.newSingleThreadScheduledExecutor());
  private Optional<MetricsProvider> metrics = Optional.empty();

  public ReactiveHermesClientBuilder(ReactiveHermesSender sender) {
    this.sender = sender;
    this.defaultHeaders.put(HermesMessage.CONTENT_TYPE_HEADER, HermesMessage.APPLICATION_JSON);
  }

  public static ReactiveHermesClientBuilder hermesClient(ReactiveHermesSender sender) {
    return new ReactiveHermesClientBuilder(sender);
  }

  public ReactiveHermesClient build() {
    ReactiveHermesClient hermesClient =
        new ReactiveHermesClient(
            sender,
            uri,
            defaultHeaders,
            retries,
            retryCondition,
            retrySleepInMillis,
            maxRetrySleepInMillis,
            jitterFactor,
            schedulerFactory.get());

    metrics.ifPresent(
        (metricsProvider) -> {
          hermesClient.addMessageDeliveryListener(
              new MetricsMessageDeliveryListener(metricsProvider));
        });

    return hermesClient;
  }

  public ReactiveHermesClientBuilder withURI(URI uri) {
    this.uri = uri;
    return this;
  }

  public ReactiveHermesClientBuilder withMetrics(MetricsProvider metrics) {
    this.metrics = Optional.of(metrics);
    return this;
  }

  public ReactiveHermesClientBuilder withDefaultContentType(String defaultContentType) {
    defaultHeaders.put(HermesMessage.CONTENT_TYPE_HEADER, defaultContentType);
    return this;
  }

  public ReactiveHermesClientBuilder withDefaultHeaderValue(String header, String value) {
    defaultHeaders.put(header, value);
    return this;
  }

  public ReactiveHermesClientBuilder withRetries(int retries) {
    this.retries = retries;
    return this;
  }

  public ReactiveHermesClientBuilder withRetries(
      int retries, Predicate<HermesResponse> retryCondition) {
    this.retryCondition = retryCondition;
    return withRetries(retries);
  }

  public ReactiveHermesClientBuilder withRetrySleep(long retrySleepInMillis) {
    this.retrySleepInMillis = retrySleepInMillis;
    return this;
  }

  public ReactiveHermesClientBuilder withRetrySleep(
      long retrySleepInMillis, long maxRetrySleepInMillis) {
    this.retrySleepInMillis = retrySleepInMillis;
    this.maxRetrySleepInMillis = maxRetrySleepInMillis;
    return this;
  }

  public ReactiveHermesClientBuilder withScheduler(Scheduler scheduler) {
    this.schedulerFactory = () -> scheduler;
    return this;
  }

  public ReactiveHermesClientBuilder withJitter(Double jitterFactor) {
    this.jitterFactor = jitterFactor;
    return this;
  }
}
