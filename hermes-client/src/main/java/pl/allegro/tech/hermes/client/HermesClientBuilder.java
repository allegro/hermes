package pl.allegro.tech.hermes.client;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Predicate;
import java.util.function.Supplier;
import pl.allegro.tech.hermes.client.metrics.MetricsMessageDeliveryListener;
import pl.allegro.tech.hermes.client.metrics.MetricsProvider;

public class HermesClientBuilder {

  private HermesSender sender;
  private URI uri = URI.create("http://localhost:8080");
  private final Map<String, String> defaultHeaders = new HashMap<>();
  private int retries = 3;
  private Predicate<HermesResponse> retryCondition = new HermesClientBasicRetryCondition();
  private long retrySleepInMillis = 100;
  private long maxRetrySleepInMillis = 300;
  private Supplier<ScheduledExecutorService> schedulerFactory =
      Executors::newSingleThreadScheduledExecutor;
  private Optional<MetricsProvider> metrics = Optional.empty();

  public HermesClientBuilder(HermesSender sender) {
    this.sender = sender;
    this.defaultHeaders.put(HermesMessage.CONTENT_TYPE_HEADER, HermesMessage.APPLICATION_JSON);
  }

  public static HermesClientBuilder hermesClient(HermesSender sender) {
    return new HermesClientBuilder(sender);
  }

  public HermesClient build() {
    HermesClient hermesClient =
        new HermesClient(
            sender,
            uri,
            defaultHeaders,
            retries,
            retryCondition,
            retrySleepInMillis,
            maxRetrySleepInMillis,
            schedulerFactory.get());

    metrics.ifPresent(
        (metricsProvider) -> {
          hermesClient.addMessageDeliveryListener(
              new MetricsMessageDeliveryListener(metricsProvider));
        });

    return hermesClient;
  }

  public HermesClientBuilder withURI(URI uri) {
    this.uri = uri;
    return this;
  }

  public HermesClientBuilder withMetrics(MetricsProvider metrics) {
    this.metrics = Optional.of(metrics);
    return this;
  }

  public HermesClientBuilder withDefaultContentType(String defaultContentType) {
    defaultHeaders.put(HermesMessage.CONTENT_TYPE_HEADER, defaultContentType);
    return this;
  }

  public HermesClientBuilder withDefaultHeaderValue(String header, String value) {
    defaultHeaders.put(header, value);
    return this;
  }

  public HermesClientBuilder withRetries(int retries) {
    this.retries = retries;
    return this;
  }

  public HermesClientBuilder withRetries(int retries, Predicate<HermesResponse> retryCondition) {
    this.retryCondition = retryCondition;
    return withRetries(retries);
  }

  public HermesClientBuilder withRetrySleep(long retrySleepInMillis) {
    this.retrySleepInMillis = retrySleepInMillis;
    return this;
  }

  public HermesClientBuilder withRetrySleep(long retrySleepInMillis, long maxRetrySleepInMillis) {
    this.retrySleepInMillis = retrySleepInMillis;
    this.maxRetrySleepInMillis = maxRetrySleepInMillis;
    return this;
  }

  public HermesClientBuilder withScheduler(ScheduledExecutorService scheduler) {
    this.schedulerFactory = () -> scheduler;
    return this;
  }
}
