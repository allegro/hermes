package pl.allegro.tech.hermes.client;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.IntStream.range;

public class HermesClient {

    private static final String APPLICATION_JSON = "application/json;charset=UTF-8";
    private static final String AVRO_BINARY = "avro/binary";

    private final HermesSender sender;
    private final String uri;
    private final String defaultContentType;
    private final int retries;
    private final Predicate<HermesResponse> retryCondition;

    HermesClient(HermesSender sender, URI uri, String defaultContentType, int retries, Predicate<HermesResponse> retryCondition) {
        this.sender = sender;
        this.uri = uri.toString() + "/topics/";
        this.defaultContentType = defaultContentType;
        this.retries = retries;
        this.retryCondition = retryCondition;
    }

    public CompletableFuture<HermesResponse> publishJSON(String topic, byte[] message) {
        return publish(topic, APPLICATION_JSON, message);
    }

    public CompletableFuture<HermesResponse> publishJSON(String topic, String message) {
        return publish(topic, APPLICATION_JSON, message);
    }

    public CompletableFuture<HermesResponse> publishAvro(String topic, int schemaVersion, byte[] message) {
        return publish(topic, AVRO_BINARY, schemaVersion, message);
    }

    public CompletableFuture<HermesResponse> publish(String topic, String message) {
        return publish(topic, defaultContentType, message);
    }

    public CompletableFuture<HermesResponse> publish(String topic, String contentType, byte[] message) {
        return publish(new HermesMessage(topic, contentType, message));
    }

    public CompletableFuture<HermesResponse> publish(String topic, String contentType, String message) {
        return publish(new HermesMessage(topic, contentType, message));
    }

    public CompletableFuture<HermesResponse> publish(String topic, String contentType, int schemaVersion, byte[] message) {
        return publish(new HermesMessage(topic, contentType, schemaVersion, message));
    }

    public CompletableFuture<HermesResponse> publish(HermesMessage message) {
        HermesMessage messageWithContent = message.getContentType() == null ? HermesMessage.appendContentType(message, defaultContentType) : message;
        return publish(messageWithContent, (response) -> retryCondition.test(response) ? sendOnce(messageWithContent) : completedFuture(response));
    }

    private CompletableFuture<HermesResponse> publish(HermesMessage message, Function<HermesResponse, CompletionStage<HermesResponse>> retryDecision) {
        return range(0, retries).boxed().reduce(sendOnce(message), (future, attempt) -> future.thenCompose(retryDecision), (future, attempt) -> future);
    }

    private CompletableFuture<HermesResponse> sendOnce(HermesMessage message) {
        return sender.send(URI.create(uri + message.getTopic()), message).exceptionally(HermesResponseBuilder::hermesFailureResponse);
    }

}
