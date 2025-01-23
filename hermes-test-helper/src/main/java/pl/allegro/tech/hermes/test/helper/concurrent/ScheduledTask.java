package pl.allegro.tech.hermes.test.helper.concurrent;

import jakarta.annotation.Nonnull;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class ScheduledTask<T> implements ScheduledFuture<T> {

  private final Callable<T> callable;
  private final long delay;
  private final CompletableFuture<T> result;

  public ScheduledTask(Callable<T> callable, long delay) {
    this.callable = Objects.requireNonNull(callable);
    this.result = new CompletableFuture<>();
    this.delay = delay;
  }

  public void execute() {
    if (!result.isDone()) {
      try {
        callable.call();
      } catch (Exception e) {
        result.completeExceptionally(e);
      }
    }
  }

  @Override
  public long getDelay(TimeUnit unit) {
    return unit.convert(delay, TimeUnit.MILLISECONDS);
  }

  @Override
  public int compareTo(Delayed o) {
    return Long.compare(getDelay(TimeUnit.MILLISECONDS), o.getDelay(TimeUnit.MILLISECONDS));
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return result.cancel(mayInterruptIfRunning);
  }

  @Override
  public boolean isCancelled() {
    return result.isCancelled();
  }

  @Override
  public boolean isDone() {
    return result.isDone();
  }

  @Override
  public T get() throws InterruptedException, ExecutionException {
    return result.get();
  }

  @Override
  public T get(long timeout, @Nonnull TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return result.get(timeout, unit);
  }
}
