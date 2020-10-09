package pl.allegro.tech.hermes.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static pl.allegro.tech.hermes.client.HermesMessage.hermesMessage;
import static reactor.core.Exceptions.isRetryExhausted;


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
    private final Scheduler scheduler;
    private volatile boolean shutdown = false;
    private final List<MessageDeliveryListener> messageDeliveryListeners = new ArrayList<>();

    ReactiveHermesClient(ReactiveHermesSender sender,
                         URI uri,
                         Map<String, String> defaultHeaders,
                         int maxRetries,
                         Predicate<HermesResponse> retryCondition,
                         long retrySleepInMillis,
                         long maxRetrySleepInMillis,
                         ScheduledExecutorService scheduler) {
        this.sender = sender;
        this.uri = createUri(uri);
        this.defaultHeaders = Collections.unmodifiableMap(new HashMap<>(defaultHeaders));
        this.maxRetries = maxRetries;
        this.retryCondition = retryCondition;
        this.retrySleep = Duration.ofMillis(retrySleepInMillis);
        this.maxRetrySleep = Duration.ofMillis(maxRetrySleepInMillis);
        this.scheduler = Schedulers.fromExecutor(scheduler);
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

    public Mono<HermesResponse> publish(String topic, String contentType, int schemaVersion, byte[] message) {
        return publish(hermesMessage(topic, message).withContentType(contentType).withSchemaVersion(schemaVersion).build());
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

        Retry retry = prepareRetry(message);

        return sendOnce(message)
                .flatMap(response -> getNextAttempt()
                        .map(attempt -> {
                            if (retryCondition.test(response)) {
                                return Result.failure(new ShouldRetryException(response), attempt);
                            } else {
                                return Result.attempt(response, attempt);
                            }
                        })
                )
                .onErrorResume(throwable -> getNextAttempt().map(attempt -> Result.failure(throwable, attempt)))
                .subscribeOn(scheduler)
                .flatMap(result -> {
                    if (result instanceof Failed) {
                        Failed failed = (Failed) result;
                        return Mono.error(new RetryFailedException(failed.attempt, failed.cause));
                    } else {
                        return Mono.just((Attempt) result);
                    }
                })
                .retryWhen(retry)
                .subscriberContext(ctx -> ctx.put(RETRY_CONTEXT_KEY, HermesRetryContext.emptyRetryContext()))
                .onErrorResume(Exception.class, exception -> {
                    if (isRetryExhausted(exception)) {
                        RetryFailedException rfe = (RetryFailedException) (exception.getCause());
                        handleMaxRetriesExceeded(message, rfe.attempt);
                        Throwable cause = rfe.getCause();
                        if (cause instanceof ShouldRetryException) {
                            ShouldRetryException sre = (ShouldRetryException) cause;
                            return Mono.just((Attempt) Result.attempt(sre.hermesResponse, rfe.attempt));
                        }
                        handleFailure(message, rfe.getAttempt());
                    }
                    return Mono.error(exception);
                })
                .doOnSuccess(hr -> {
                    if (hr.response.isSuccess()) {
                        handleSuccessfulRetry(message, hr.getAttempt());
                    } else {
                        handleFailure(message, hr.getAttempt());
                    }
                })
                .map(result -> result.response)
                .doFinally(s -> currentlySending.decrementAndGet());
    }

    private Retry prepareRetry(HermesMessage message) {
        if (!retrySleep.isZero()) {
            return Retry.max(maxRetries)
                    .doAfterRetry(signal -> handleFailedAttempt(message, signal.totalRetries() + 1));
        } else {
            return Retry.backoff(maxRetries, retrySleep)
                    .maxBackoff(maxRetrySleep)
                    .doAfterRetry(signal -> handleFailedAttempt(message, signal.totalRetries() + 1));
        }
    }

    private Mono<Integer> getNextAttempt() {
        return Mono.subscriberContext()
                .map(ctx -> ctx.getOrDefault(RETRY_CONTEXT_KEY, HermesRetryContext.emptyRetryContext())
                        .getAndIncrementAttempt()
                );
    }

    private Mono<HermesResponse> sendOnce(HermesMessage message) {
        return Mono.defer(() -> {
                    long startTime = System.nanoTime();
                    try {
                        return sender.sendReactively(URI.create(uri + message.getTopic()), message)
                                .onErrorResume(e -> Mono.just(HermesResponseBuilder.hermesFailureResponse(e, message)))
                                .doOnNext(resp -> {
                                    long latency = System.nanoTime() - startTime;
                                    messageDeliveryListeners.forEach(l -> l.onSend(resp, latency));
                                });
                    } catch (Exception e) {
                        return Mono.error(e);
                    }
                }
        );
    }

    private Mono<HermesResponse> completedWithShutdownException() {
        return Mono.error(new HermesClientShutdownException());
    }

    public Mono<Void> closeAsync(long pollInterval) {
        shutdown = true;
        CompletableFuture<Void> voidCompletableFuture = new HermesClientTermination(pollInterval)
                .observe(() -> currentlySending.get() == 0)
                .whenComplete((response, ex) -> scheduler.dispose());
        return Mono.fromFuture(voidCompletableFuture);
    }

    public void close(long pollInterval, long timeout) {
        closeAsync(pollInterval).block(Duration.ofMillis(timeout));
    }

    private void handleMaxRetriesExceeded(HermesMessage message, int attemptCount) {
        messageDeliveryListeners.forEach(l -> l.onMaxRetriesExceeded(message, attemptCount));
        logger.error("Failed to send message to topic {} after {} attempts",
                message.getTopic(), attemptCount);
    }

    private void handleFailedAttempt(HermesMessage message, long attemptCount) {
        messageDeliveryListeners.forEach(l -> l.onFailedRetry(message, (int) attemptCount));
    }

    private void handleFailure(HermesMessage message, long attemptCount) {
        messageDeliveryListeners.forEach(l -> l.onFailure(message, (int) attemptCount));
    }

    private void handleSuccessfulRetry(HermesMessage message, long attemptCount) {
        messageDeliveryListeners.forEach(l -> l.onSuccessfulRetry(message, (int) attemptCount));
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
        private final int attempt;

        public RetryFailedException(int attempt, Throwable cause) {
            super(cause);
            this.attempt = attempt;
        }

        public long getAttempt() {
            return attempt;
        }
    }

    private interface Result {
        static Result attempt(HermesResponse response, int attempt) {
            return new Attempt(response, attempt);
        }

        static Result failure(Throwable cause, int attempt) {
            return new Failed(attempt, cause);
        }
    }

    private static class Attempt implements Result {
        private final HermesResponse response;
        private final int attempt;

        private Attempt(HermesResponse response, int attempt) {
            this.response = response;
            this.attempt = attempt;
        }

        public long getAttempt() {
            return attempt;
        }

        public HermesResponse getMessage() {
            return response;
        }
    }

    private static class Failed implements Result {
        private final int attempt;
        private final Throwable cause;

        private Failed(int attempt, Throwable cause) {
            this.attempt = attempt;
            this.cause = cause;
        }

        public long getRetries() {
            return attempt;
        }

        public Throwable getCause() {
            return cause;
        }
    }

    private static class HermesRetryContext {
        private int attempt;

        HermesRetryContext() {
            attempt = 1;
        }

        static HermesRetryContext emptyRetryContext() {
            return new HermesRetryContext();
        }

        int getAndIncrementAttempt() {
            return attempt++;
        }
    }
}
