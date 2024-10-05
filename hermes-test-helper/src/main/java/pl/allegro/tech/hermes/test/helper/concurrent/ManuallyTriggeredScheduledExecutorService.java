package pl.allegro.tech.hermes.test.helper.concurrent;

import jakarta.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ManuallyTriggeredScheduledExecutorService implements ScheduledExecutorService {

  private final ConcurrentLinkedQueue<ScheduledTask<?>> scheduledTasks =
      new ConcurrentLinkedQueue<>();
  private boolean shutdown;

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(
      @Nonnull Runnable command, long initialDelay, long period, @Nonnull TimeUnit unit) {
    return insertTask(command, initialDelay, unit);
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(
      @Nonnull Runnable command, long initialDelay, long delay, @Nonnull TimeUnit unit) {
    return insertTask(command, initialDelay, unit);
  }

  @Override
  public void shutdown() {
    shutdown = true;
  }

  @Override
  public List<Runnable> shutdownNow() {
    shutdown();
    return List.of();
  }

  @Override
  public boolean isShutdown() {
    return false;
  }

  @Override
  public boolean isTerminated() {
    return shutdown;
  }

  @Override
  public boolean awaitTermination(long timeout, @Nonnull TimeUnit unit) {
    return true;
  }

  @Override
  public ScheduledFuture<?> schedule(
      @Nonnull Runnable command, long delay, @Nonnull TimeUnit unit) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <V> ScheduledFuture<V> schedule(
      @Nonnull Callable<V> callable, long delay, @Nonnull TimeUnit unit) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void execute(@Nonnull Runnable command) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> Future<T> submit(@Nonnull Callable<T> task) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> Future<T> submit(@Nonnull Runnable task, T result) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Future<?> submit(@Nonnull Runnable task) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> List<Future<T>> invokeAll(@Nonnull Collection<? extends Callable<T>> tasks) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> List<Future<T>> invokeAll(
      @Nonnull Collection<? extends Callable<T>> tasks, long timeout, @Nonnull TimeUnit unit) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T invokeAny(@Nonnull Collection<? extends Callable<T>> tasks) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T invokeAny(
      @Nonnull Collection<? extends Callable<T>> tasks, long timeout, @Nonnull TimeUnit unit) {
    throw new UnsupportedOperationException();
  }

  public void triggerScheduledTasks() {
    for (ScheduledTask<?> scheduledTask : scheduledTasks) {
      if (!scheduledTask.isCancelled()) {
        scheduledTask.execute();
      }
    }
  }

  private ScheduledFuture<?> insertTask(Runnable command, long delay, TimeUnit unit) {
    ScheduledTask<?> scheduledTask =
        new ScheduledTask<>(
            () -> {
              command.run();
              return null;
            },
            unit.convert(delay, TimeUnit.MILLISECONDS));
    scheduledTasks.offer(scheduledTask);
    return scheduledTask;
  }
}
