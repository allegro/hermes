package pl.allegro.tech.hermes.consumers.utils;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

/**
 * see http://www.nurkiewicz.com/2014/12/asynchronous-timeouts-with.html
 */
public class FutureAsyncTimeout<T> {

    private final ScheduledExecutorService executor;
    private final Function<TimeoutException, T> failure;

    public FutureAsyncTimeout(Function<TimeoutException, T> failure) {
        this.executor = Executors.newScheduledThreadPool(1,
                new ThreadFactoryBuilder().setDaemon(true).setNameFormat("ConsumerAsyncTimeout-%d").build());
        this.failure = failure;
    }

    public CompletableFuture<T> within(CompletableFuture<T> future, Duration duration) {
        return future.applyToEither(failAfter(duration), Function.identity());
    }

    private CompletableFuture<T> failAfter(Duration duration) {
        final CompletableFuture<T> promise = new CompletableFuture<>();
        executor.schedule(() -> {
            TimeoutException ex = new TimeoutException("Timeout after " + duration);
            return promise.complete(failure.apply(ex));
        }, duration.toMillis(), TimeUnit.MILLISECONDS);
        return promise;
    }

}
