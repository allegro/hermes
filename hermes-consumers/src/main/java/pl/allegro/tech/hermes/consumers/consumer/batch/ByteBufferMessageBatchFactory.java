package pl.allegro.tech.hermes.consumers.consumer.batch;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.UUID.randomUUID;

import java.nio.ByteBuffer;
import java.time.Clock;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;

public class ByteBufferMessageBatchFactory implements MessageBatchFactory {
  private final DirectBufferPool bufferPool;
  private final Clock clock;

  public ByteBufferMessageBatchFactory(
      int poolableSize, int maxPoolSize, Clock clock, MetricsFacade metrics) {
    this.clock = clock;
    this.bufferPool = new DirectBufferPool(maxPoolSize, poolableSize, true);
    metrics
        .consumer()
        .registerBatchBufferTotalBytesGauge(bufferPool, DirectBufferPool::totalMemory);
    metrics
        .consumer()
        .registerBatchBufferAvailableBytesGauge(bufferPool, DirectBufferPool::availableMemory);
  }

  @Override
  public MessageBatch createBatch(Subscription subscription) {
    try {
      ByteBuffer buffer =
          bufferPool.allocate(subscription.getBatchSubscriptionPolicy().getBatchVolume());
      switch (subscription.getContentType()) {
        case JSON:
          return new JsonMessageBatch(randomUUID().toString(), buffer, subscription, clock);
        case AVRO:
        default:
          throw new UnsupportedOperationException(
              "Batching is not supported yet for contentType " + subscription.getContentType());
      }
    } catch (InterruptedException e) {
      throw new InternalProcessingException(e);
    }
  }

  @Override
  public void destroyBatch(MessageBatch batch) {
    checkNotNull(batch);
    bufferPool.deallocate(batch.getContent());
  }
}
