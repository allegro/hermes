package pl.allegro.tech.hermes.consumers.consumer.sender.timeout;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

/** see http://www.nurkiewicz.com/2014/12/asynchronous-timeouts-with.html */
public class FutureAsyncTimeout {

  private final ScheduledExecutorService executor;

  public FutureAsyncTimeout(ScheduledExecutorService scheduledExecutorService) {
    this.executor = scheduledExecutorService;
  }

  public <T> CompletableFuture<T> within(
      CompletableFuture<T> future, Duration duration, Function<Throwable, T> exceptionMapper) {
    return future.applyToEither(failAfter(duration, exceptionMapper), Function.identity());
  }

  private <T> CompletableFuture<T> failAfter(
      Duration duration, Function<Throwable, T> exceptionMapper) {
    final CompletableFuture<T> promise = new CompletableFuture<>();
    executor.schedule(
        () -> {
          TimeoutException ex = new TimeoutException("Timeout after " + duration);
          return promise.complete(exceptionMapper.apply(ex));
        },
        duration.toMillis(),
        TimeUnit.MILLISECONDS);
    return promise;
  }

  public void shutdown() {
    executor.shutdown();
  }
}
