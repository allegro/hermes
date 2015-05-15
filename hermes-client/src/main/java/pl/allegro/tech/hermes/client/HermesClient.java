package pl.allegro.tech.hermes.client;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.IntStream.range;

public class HermesClient {
    private final HermesSender sender;
    private final int retries;
    private final Predicate<HermesResponse> retryCondition;
    private final String uri;

    HermesClient(HermesSender sender, URI uri, int retries, Predicate<HermesResponse> retryCondition) {
        this.sender = sender;
        this.retries = retries;
        this.retryCondition = retryCondition;
        this.uri = uri.toString() + "/topics/";
    }

    public CompletableFuture<HermesResponse> publish(String topic, String message) {
        return publish(new HermesMessage(topic, message));
    }

    public CompletableFuture<HermesResponse> publish(HermesMessage message) {
        return publish(message, (response) -> retryCondition.test(response) ? sendOnce(message) : completedFuture(response));
    }

    private CompletableFuture<HermesResponse> publish(HermesMessage message, Function<HermesResponse, CompletionStage<HermesResponse>> λ) {
        return range(0, retries).boxed().reduce(sendOnce(message), (future, $) -> future.thenCompose(λ), (future, $) -> future);
    }

    private CompletableFuture<HermesResponse> sendOnce(HermesMessage message) {
        return sender.send(URI.create(uri + message.getTopic()), message);
    }

}
