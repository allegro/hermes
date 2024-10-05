package pl.allegro.tech.hermes.client;

import static pl.allegro.tech.hermes.client.HermesMessage.hermesMessage;

import java.net.URI;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import net.jodah.failsafe.event.ExecutionAttemptedEvent;
import net.jodah.failsafe.event.ExecutionCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HermesClient {
  private static final Logger logger = LoggerFactory.getLogger(HermesClient.class);

  private final HermesSender sender;
  private final String uri;
  private final Map<String, String> defaultHeaders;
  private final AtomicInteger currentlySending = new AtomicInteger(0);
  private final RetryPolicy<HermesResponse> retryPolicy;
  private final ScheduledExecutorService scheduler;
  private volatile boolean shutdown = false;
  private final List<MessageDeliveryListener> messageDeliveryListeners = new ArrayList<>();

  HermesClient(
      HermesSender sender,
      URI uri,
      Map<String, String> defaultHeaders,
      int retries,
      Predicate<HermesResponse> retryCondition,
      long retrySleepInMillis,
      long maxRetrySleepInMillis,
      ScheduledExecutorService scheduler) {
    this.sender = sender;
    this.uri = createUri(uri);
    this.defaultHeaders = Collections.unmodifiableMap(new HashMap<>(defaultHeaders));
    this.retryPolicy =
        createRetryPolicy(retries, retryCondition, retrySleepInMillis, maxRetrySleepInMillis);
    this.scheduler = scheduler;
  }

  private RetryPolicy<HermesResponse> createRetryPolicy(
      int retries,
      Predicate<HermesResponse> retryCondition,
      long retrySleepInMillis,
      long maxRetrySleepInMillis) {
    RetryPolicy<HermesResponse> retryPolicy =
        new RetryPolicy<HermesResponse>()
            .withMaxRetries(retries)
            .handleIf((resp, cause) -> retryCondition.test(resp))
            .onRetriesExceeded((e) -> handleMaxRetriesExceeded(e))
            .onRetry((e) -> handleFailedAttempt(e))
            .onFailure((e) -> handleFailure(e))
            .onSuccess((e) -> handleSuccessfulRetry(e));

    if (retrySleepInMillis > 0) {
      retryPolicy.withBackoff(retrySleepInMillis, maxRetrySleepInMillis, ChronoUnit.MILLIS);
    }
    return retryPolicy;
  }

  private String createUri(URI uri) {
    String uriString = uri.toString();
    return uriString + (uriString.endsWith("/") ? "" : "/") + "topics/";
  }

  public CompletableFuture<HermesResponse> publishJSON(String topic, byte[] message) {
    return publish(hermesMessage(topic, message).json().build());
  }

  public CompletableFuture<HermesResponse> publishJSON(String topic, String message) {
    return publish(hermesMessage(topic, message).json().build());
  }

  public CompletableFuture<HermesResponse> publishAvro(
      String topic, int schemaVersion, byte[] message) {
    return publish(hermesMessage(topic, message).avro(schemaVersion).build());
  }

  public CompletableFuture<HermesResponse> publish(String topic, String message) {
    return publish(hermesMessage(topic, message).build());
  }

  public CompletableFuture<HermesResponse> publish(
      String topic, String contentType, byte[] message) {
    return publish(hermesMessage(topic, message).withContentType(contentType).build());
  }

  public CompletableFuture<HermesResponse> publish(
      String topic, String contentType, String message) {
    return publish(hermesMessage(topic, message).withContentType(contentType).build());
  }

  public CompletableFuture<HermesResponse> publish(
      String topic, String contentType, int schemaVersion, byte[] message) {
    return publish(
        hermesMessage(topic, message)
            .withContentType(contentType)
            .withSchemaVersion(schemaVersion)
            .build());
  }

  public CompletableFuture<HermesResponse> publish(HermesMessage message) {
    if (shutdown) {
      return completedWithShutdownException();
    }
    HermesMessage.appendDefaults(message, defaultHeaders);
    return publishWithRetries(message);
  }

  public boolean addMessageDeliveryListener(MessageDeliveryListener listener) {
    return messageDeliveryListeners.add(listener);
  }

  private CompletableFuture<HermesResponse> publishWithRetries(HermesMessage message) {
    currentlySending.incrementAndGet();
    return Failsafe.with(retryPolicy)
        .with(scheduler)
        .onComplete((e) -> currentlySending.decrementAndGet())
        .getStageAsync(() -> sendOnce(message));
  }

  private CompletableFuture<HermesResponse> sendOnce(HermesMessage message) {
    long startTime = System.nanoTime();

    return sender
        .send(URI.create(uri + message.getTopic()), message)
        .exceptionally(e -> HermesResponseBuilder.hermesFailureResponse(e, message))
        .whenComplete(
            (resp, cause) -> {
              long latency = System.nanoTime() - startTime;
              messageDeliveryListeners.forEach(l -> l.onSend(resp, latency));
            });
  }

  private CompletableFuture<HermesResponse> completedWithShutdownException() {
    CompletableFuture<HermesResponse> alreadyShutdown = new CompletableFuture<>();
    alreadyShutdown.completeExceptionally(new HermesClientShutdownException());
    return alreadyShutdown;
  }

  public CompletableFuture<Void> closeAsync(long pollInterval) {
    shutdown = true;
    return new HermesClientTermination(pollInterval)
        .observe(() -> currentlySending.get() == 0)
        .whenComplete((response, ex) -> scheduler.shutdown());
  }

  public void close(long pollInterval, long timeout) throws InterruptedException, TimeoutException {
    try {
      closeAsync(pollInterval).get(timeout, TimeUnit.MILLISECONDS);
    } catch (ExecutionException e) {
      throw (InterruptedException) e.getCause();
    }
  }

  private void handleMaxRetriesExceeded(ExecutionCompletedEvent<HermesResponse> event) {
    if (event.getResult().isSuccess()) {
      return;
    }

    HermesMessage message = event.getResult().getHermesMessage();
    messageDeliveryListeners.forEach(
        l -> l.onMaxRetriesExceeded(event.getResult(), event.getAttemptCount()));
    logger.error(
        "Failed to send message to topic {} after {} attempts",
        message.getTopic(),
        event.getAttemptCount());
  }

  private void handleFailedAttempt(ExecutionAttemptedEvent<HermesResponse> event) {
    messageDeliveryListeners.forEach(
        l -> l.onFailedRetry(event.getLastResult(), event.getAttemptCount()));
  }

  private void handleFailure(ExecutionCompletedEvent<HermesResponse> event) {
    messageDeliveryListeners.forEach(l -> l.onFailure(event.getResult(), event.getAttemptCount()));
  }

  private void handleSuccessfulRetry(ExecutionCompletedEvent<HermesResponse> event) {
    messageDeliveryListeners.forEach(
        l -> l.onSuccessfulRetry(event.getResult(), event.getAttemptCount()));
  }
}
