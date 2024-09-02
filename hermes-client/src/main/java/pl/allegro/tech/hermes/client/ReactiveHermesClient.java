package pl.allegro.tech.hermes.client;

import static pl.allegro.tech.hermes.client.HermesMessage.hermesMessage;
import static pl.allegro.tech.hermes.client.HermesResponseBuilder.hermesFailureResponse;
import static reactor.core.Exceptions.isRetryExhausted;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.util.retry.Retry;

public class ReactiveHermesClient {
  private static final Logger logger = LoggerFactory.getLogger(ReactiveHermesClient.class);
  private static final String RETRY_CONTEXT_KEY = "hermes-retry-context-key";

  private final ReactiveHermesSender sender;
  private final String uri;
  private final Map<String, String> defaultHeaders;
  private final AtomicInteger currentlySending = new AtomicInteger(0);
  private final int maxRetries;
  private final Predicate<HermesResponse> retryCondition;
  private final Duration retrySleep;
  private final Duration maxRetrySleep;
  private final Double jitterFactor;
  private final Scheduler scheduler;
  private volatile boolean shutdown = false;
  private final List<MessageDeliveryListener> messageDeliveryListeners;

  ReactiveHermesClient(
      ReactiveHermesSender sender,
      URI uri,
      Map<String, String> defaultHeaders,
      int maxRetries,
      Predicate<HermesResponse> retryCondition,
      long retrySleepInMillis,
      long maxRetrySleepInMillis,
      double jitterFactor,
      Scheduler scheduler) {
    this.sender = sender;
    this.uri = createUri(uri);
    this.defaultHeaders = Collections.unmodifiableMap(new HashMap<>(defaultHeaders));
    this.maxRetries = maxRetries;
    this.retryCondition = retryCondition;
    this.retrySleep = Duration.ofMillis(retrySleepInMillis);
    this.maxRetrySleep = Duration.ofMillis(maxRetrySleepInMillis);
    this.jitterFactor = jitterFactor;
    this.scheduler = scheduler;
    this.messageDeliveryListeners = new ArrayList<>();
  }

  private String createUri(URI uri) {
    String uriString = uri.toString();
    return uriString + (uriString.endsWith("/") ? "" : "/") + "topics/";
  }

  public Mono<HermesResponse> publishJSON(String topic, byte[] message) {
    return publish(hermesMessage(topic, message).json().build());
  }

  public Mono<HermesResponse> publishJSON(String topic, String message) {
    return publish(hermesMessage(topic, message).json().build());
  }

  public Mono<HermesResponse> publishAvro(String topic, int schemaVersion, byte[] message) {
    return publish(hermesMessage(topic, message).avro(schemaVersion).build());
  }

  public Mono<HermesResponse> publish(String topic, String message) {
    return publish(hermesMessage(topic, message).build());
  }

  public Mono<HermesResponse> publish(String topic, String contentType, byte[] message) {
    return publish(hermesMessage(topic, message).withContentType(contentType).build());
  }

  public Mono<HermesResponse> publish(String topic, String contentType, String message) {
    return publish(hermesMessage(topic, message).withContentType(contentType).build());
  }

  public Mono<HermesResponse> publish(
      String topic, String contentType, int schemaVersion, byte[] message) {
    return publish(
        hermesMessage(topic, message)
            .withContentType(contentType)
            .withSchemaVersion(schemaVersion)
            .build());
  }

  public Mono<HermesResponse> publish(HermesMessage message) {
    if (shutdown) {
      return completedWithShutdownException();
    }
    HermesMessage.appendDefaults(message, defaultHeaders);
    return publishWithRetries(message);
  }

  public boolean addMessageDeliveryListener(MessageDeliveryListener listener) {
    return messageDeliveryListeners.add(listener);
  }

  private Mono<HermesResponse> publishWithRetries(HermesMessage message) {
    currentlySending.incrementAndGet();
    Mono<Result> sendOnceResult =
        sendOnce(message)
            .flatMap(this::testRetryCondition)
            .onErrorResume(exception -> mapExceptionToFailedAttempt(exception, message))
            .subscribeOn(scheduler);

    return retry(sendOnceResult)
        .doOnSuccess(
            hr -> {
              if (hr.response.isSuccess() || !hr.matchesRetryPolicy) {
                handleSuccessfulRetry(hr.response, hr.attempt);
              } else {
                handleFailure(hr.response, hr.attempt);
              }
            })
        .map(result -> result.response)
        .doFinally(s -> currentlySending.decrementAndGet());
  }

  private Mono<Attempt> retry(Mono<Result> sendOnceResult) {
    Retry retrySpec = prepareRetrySpec();
    return sendOnceResult
        .flatMap(this::unwrapFailedAttemptAsException)
        .retryWhen(retrySpec)
        .contextWrite(ctx -> ctx.put(RETRY_CONTEXT_KEY, HermesRetryContext.emptyRetryContext()))
        .onErrorResume(Exception.class, this::unwrapRetryExhaustedException);
  }

  private Mono<Attempt> unwrapRetryExhaustedException(Exception exception) {
    if (isRetryExhausted(exception)) {
      RetryFailedException rfe = (RetryFailedException) (exception.getCause());
      Failed failedAttempt = rfe.failed;
      HermesResponse hermesResponse = failedAttempt.hermesResponse;
      handleMaxRetriesExceeded(hermesResponse, failedAttempt.attempt);
      Throwable cause = rfe.getCause();
      if (cause instanceof ShouldRetryException) {
        ShouldRetryException sre = (ShouldRetryException) cause;
        return Mono.just((Attempt) Result.attempt(sre.hermesResponse, failedAttempt.attempt, true));
      }
      if (hermesResponse.isFailure()) {
        handleFailure(hermesResponse, failedAttempt.attempt);
      }
    }
    return Mono.error(exception);
  }

  private Mono<Attempt> unwrapFailedAttemptAsException(Result result) {
    if (result instanceof Failed) {
      Failed failed = (Failed) result;
      return Mono.error(new RetryFailedException(failed));
    } else {
      return Mono.just((Attempt) result);
    }
  }

  private Mono<Result> mapExceptionToFailedAttempt(
      Throwable throwable, HermesMessage hermesMessage) {
    return getNextAttempt()
        .map(attempt -> Result.failureByException(throwable, hermesMessage, attempt));
  }

  private Mono<Result> testRetryCondition(HermesResponse response) {
    return getNextAttempt()
        .map(
            attempt -> {
              if (retryCondition.test(response)) {
                return Result.retryableFailure(response, attempt);
              } else {
                return Result.attempt(response, attempt, false);
              }
            });
  }

  private Retry prepareRetrySpec() {
    if (retrySleep.isZero()) {
      return Retry.max(maxRetries).doAfterRetry(this::handleRetryAttempt);
    } else {
      return Retry.backoff(maxRetries, retrySleep)
          .maxBackoff(maxRetrySleep)
          .jitter(jitterFactor)
          .doAfterRetry(this::handleRetryAttempt);
    }
  }

  private void handleRetryAttempt(Retry.RetrySignal retrySignal) {
    RetryFailedException failedException = (RetryFailedException) retrySignal.failure();
    handleFailedAttempt(failedException.failed.hermesResponse, retrySignal.totalRetries() + 1);
  }

  private Mono<Integer> getNextAttempt() {
    return Mono.deferContextual(Mono::just)
        .map(
            ctx ->
                ctx.getOrDefault(RETRY_CONTEXT_KEY, HermesRetryContext.emptyRetryContext())
                    .getAndIncrementAttempt());
  }

  private Mono<HermesResponse> sendOnce(HermesMessage message) {
    return Mono.defer(
        () -> {
          long startTime = System.nanoTime();
          try {
            return sender
                .sendReactively(URI.create(uri + message.getTopic()), message)
                .onErrorResume(e -> Mono.just(hermesFailureResponse(e, message)))
                .doOnNext(
                    resp -> {
                      long latency = System.nanoTime() - startTime;
                      messageDeliveryListeners.forEach(l -> l.onSend(resp, latency));
                    });

          } catch (Exception e) {
            return Mono.error(e);
          }
        });
  }

  private Mono<HermesResponse> completedWithShutdownException() {
    return Mono.error(new HermesClientShutdownException());
  }

  public Mono<Void> closeAsync(long pollInterval) {
    shutdown = true;
    CompletableFuture<Void> voidCompletableFuture =
        new HermesClientTermination(pollInterval)
            .observe(() -> currentlySending.get() == 0)
            .whenComplete((response, ex) -> scheduler.dispose());
    return Mono.fromFuture(voidCompletableFuture);
  }

  public void close(long pollInterval, long timeout) {
    closeAsync(pollInterval).block(Duration.ofMillis(timeout));
  }

  private void handleMaxRetriesExceeded(HermesResponse response, int attemptCount) {
    messageDeliveryListeners.forEach(l -> l.onMaxRetriesExceeded(response, attemptCount));
    logger.error(
        "Failed to send message to topic {} after {} attempts",
        response.getHermesMessage().getTopic(),
        attemptCount);
  }

  private void handleFailedAttempt(HermesResponse response, long attemptCount) {
    messageDeliveryListeners.forEach(l -> l.onFailedRetry(response, (int) attemptCount));
  }

  private void handleFailure(HermesResponse response, long attemptCount) {
    messageDeliveryListeners.forEach(l -> l.onFailure(response, (int) attemptCount));
  }

  private void handleSuccessfulRetry(HermesResponse response, long attemptCount) {
    messageDeliveryListeners.forEach(l -> l.onSuccessfulRetry(response, (int) attemptCount));
  }

  private static class ShouldRetryException extends Exception {
    private final HermesResponse hermesResponse;

    public ShouldRetryException(HermesResponse hermesResponse) {
      this.hermesResponse = hermesResponse;
    }

    public HermesResponse getHermesResponse() {
      return hermesResponse;
    }
  }

  private static class RetryFailedException extends Exception {
    private final Failed failed;

    public RetryFailedException(Failed failed) {
      super(failed.cause);
      this.failed = failed;
    }
  }

  private interface Result {
    static Result attempt(HermesResponse response, int attempt, boolean qualifiedForRetry) {
      return new Attempt(response, attempt, qualifiedForRetry);
    }

    static Result retryableFailure(HermesResponse response, int attempt) {
      return new Failed(response, attempt, new ShouldRetryException(response));
    }

    static Result failureByException(
        Throwable throwable, HermesMessage hermesMessage, int attempt) {
      return new Failed(hermesFailureResponse(throwable, hermesMessage), attempt, throwable);
    }
  }

  private static class Attempt implements Result {
    private final HermesResponse response;
    private final int attempt;
    private final boolean matchesRetryPolicy;

    private Attempt(HermesResponse response, int attempt, boolean matchesRetryPolicy) {
      this.response = response;
      this.attempt = attempt;
      this.matchesRetryPolicy = matchesRetryPolicy;
    }
  }

  private static class Failed implements Result {
    private final HermesResponse hermesResponse;
    private final int attempt;
    private final Throwable cause;

    private Failed(HermesResponse hermesResponse, int attempt, Throwable cause) {
      this.attempt = attempt;
      this.cause = cause;
      this.hermesResponse = hermesResponse;
    }
  }

  private static class HermesRetryContext {
    static HermesRetryContext emptyRetryContext() {
      return new HermesRetryContext();
    }

    private int attempt;

    HermesRetryContext() {
      attempt = 1;
    }

    int getAndIncrementAttempt() {
      return attempt++;
    }
  }
}
