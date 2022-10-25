package pl.allegro.tech.hermes.consumers.consumer.sender;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public interface SendFutureProvider<T extends MessageSendingResult> {
    /**
     * Provides CompletableFuture which should be used for message sending
     * @param resultFutureConsumer consumer for the future which can be used e.g. to asynchronously complete this future with T
     * @param exceptionMapper mapping from exception to T
     * @return CompletableFuture which was accepted to resultFutureConsumer
     */
    CompletableFuture<T> provide(Consumer<CompletableFuture<T>> resultFutureConsumer, Function<Throwable, T> exceptionMapper);
}
