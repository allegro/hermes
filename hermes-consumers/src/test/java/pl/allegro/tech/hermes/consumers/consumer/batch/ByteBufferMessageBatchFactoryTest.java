package pl.allegro.tech.hermes.consumers.consumer.batch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import pl.allegro.tech.hermes.api.BatchSubscriptionPolicy;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;

public class ByteBufferMessageBatchFactoryTest {

  private static final int BATCH_VOLUME = 1024;

  @Test
  public void shouldRestoreInterruptStatusWhenBatchAllocationIsInterrupted()
      throws InterruptedException {
    // given
    ByteBufferMessageBatchFactory factory =
        new ByteBufferMessageBatchFactory(
            BATCH_VOLUME,
            BATCH_VOLUME,
            Clock.systemUTC(),
            new MetricsFacade(new SimpleMeterRegistry()));
    Subscription subscription =
        subscription("group.topic", "subscription")
            .withSubscriptionPolicy(
                new BatchSubscriptionPolicy(60, false, 500, 30_000, 10, 1_000, BATCH_VOLUME))
            .build();

    // the pool holds exactly one batch volume, so the first batch exhausts it
    MessageBatch firstBatch = factory.createBatch(subscription);

    AtomicReference<Throwable> thrown = new AtomicReference<>();
    AtomicBoolean interruptedWhenCaught = new AtomicBoolean(false);
    Thread worker =
        new Thread(
            () -> {
              try {
                factory.createBatch(subscription);
              } catch (Throwable e) {
                thrown.set(e);
                interruptedWhenCaught.set(Thread.currentThread().isInterrupted());
              }
            });

    try {
      // when: the second allocation blocks on the exhausted pool and gets interrupted
      worker.start();
      awaitBlocked(worker);
      worker.interrupt();
      worker.join(TimeUnit.SECONDS.toMillis(30));

      // then
      assertFalse("worker thread should finish after being interrupted", worker.isAlive());
      assertTrue(
          "expected InternalProcessingException but got: " + thrown.get(),
          thrown.get() instanceof InternalProcessingException);
      assertEquals(InterruptedException.class, thrown.get().getCause().getClass());
      assertTrue(
          "interrupt status should be restored before InternalProcessingException is thrown",
          interruptedWhenCaught.get());
    } finally {
      factory.destroyBatch(firstBatch);
    }
  }

  private static void awaitBlocked(Thread worker) throws InterruptedException {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
    while (System.nanoTime() < deadline) {
      Thread.State state = worker.getState();
      if (state == Thread.State.WAITING || state == Thread.State.TIMED_WAITING) {
        return;
      }
      Thread.sleep(10);
    }
    fail("worker thread did not block on the exhausted buffer pool");
  }
}
