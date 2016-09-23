package pl.allegro.tech.hermes.client;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static pl.allegro.tech.hermes.client.HermesMessage.hermesMessage;

public class HermesClient {

    private final HermesSender sender;
    private final String uri;
    private final Map<String, String> defaultHeaders;
    private final AtomicInteger currentlySending = new AtomicInteger(0);
    private final RetryPolicy retryPolicy;
    private final ScheduledExecutorService scheduler;
    private volatile boolean shutdown = false;

    HermesClient(HermesSender sender,
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
        this.retryPolicy = createRetryPolicy(retries, retryCondition, retrySleepInMillis, maxRetrySleepInMillis);
        this.scheduler = scheduler;
    }

    private RetryPolicy createRetryPolicy(int retries, Predicate<HermesResponse> retryCondition,
                                          long retrySleepInMillis, long maxRetrySleepInMillis) {
        RetryPolicy retryPolicy = new RetryPolicy().withMaxRetries(retries)
                .retryIf(retryCondition::test);
        if (retrySleepInMillis > 0) {
            retryPolicy.withBackoff(retrySleepInMillis, maxRetrySleepInMillis, TimeUnit.MILLISECONDS);
        }
        return retryPolicy;
    }

    private String createUri(URI uri) {
        String uriString = uri.toString();
        return uriString + (uriString.endsWith("/") ? "" : "/" ) + "topics/";
    }

    public CompletableFuture<HermesResponse> publishJSON(String topic, byte[] message) {
        return publish(hermesMessage(topic, message).json().build());
    }

    public CompletableFuture<HermesResponse> publishJSON(String topic, String message) {
        return publish(hermesMessage(topic, message).json().build());
    }

    public CompletableFuture<HermesResponse> publishAvro(String topic, int schemaVersion, byte[] message) {
        return publish(hermesMessage(topic, message).avro(schemaVersion).build());
    }

    public CompletableFuture<HermesResponse> publish(String topic, String message) {
        return publish(hermesMessage(topic, message).build());
    }

    public CompletableFuture<HermesResponse> publish(String topic, String contentType, byte[] message) {
        return publish(hermesMessage(topic, message).withContentType(contentType).build());
    }

    public CompletableFuture<HermesResponse> publish(String topic, String contentType, String message) {
        return publish(hermesMessage(topic, message).withContentType(contentType).build());
    }

    public CompletableFuture<HermesResponse> publish(String topic, String contentType, int schemaVersion, byte[] message) {
        return publish(hermesMessage(topic, message).withContentType(contentType).withSchemaVersion(schemaVersion).build());
    }

    public CompletableFuture<HermesResponse> publish(HermesMessage message) {
        if (shutdown) {
            return completedWithShutdownException();
        }
        HermesMessage.appendDefaults(message, defaultHeaders);
        return publishWithRetries(message);
    }

    private CompletableFuture<HermesResponse> publishWithRetries(HermesMessage message) {
        currentlySending.incrementAndGet();
        return Failsafe.with(retryPolicy)
                .with(scheduler)
                .future(() -> sendOnce(message))
                .whenComplete((response, ex) -> currentlySending.decrementAndGet());
    }

    private CompletableFuture<HermesResponse> sendOnce(HermesMessage message) {
        return sender.send(URI.create(uri + message.getTopic()), message).exceptionally(HermesResponseBuilder::hermesFailureResponse);
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

}
