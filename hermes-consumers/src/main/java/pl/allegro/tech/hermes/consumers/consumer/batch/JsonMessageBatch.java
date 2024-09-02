package pl.allegro.tech.hermes.consumers.consumer.batch;

import static com.google.common.base.Preconditions.checkState;
import static pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset.subscriptionPartitionOffset;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.concurrent.NotThreadSafe;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Header;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset;
import pl.allegro.tech.hermes.tracker.consumers.MessageMetadata;

@NotThreadSafe
public class JsonMessageBatch implements MessageBatch {

  private final Clock clock;

  private final int maxBatchTime;
  private final int batchSize;

  private final String id;
  private final String topic;
  private final SubscriptionName subscription;
  private final boolean hasSubscriptionIdentityHeaders;
  private final ByteBuffer byteBuffer;
  private final List<MessageMetadata> metadata = new ArrayList<>();
  private final List<Header> additionalHeaders;

  private int elements = 0;
  private long batchStart;
  private boolean closed = false;
  private int retryCounter = 0;

  JsonMessageBatch(String id, ByteBuffer buffer, Subscription subscription, Clock clock) {
    this.id = id;
    this.clock = clock;
    this.maxBatchTime = subscription.getBatchSubscriptionPolicy().getBatchTime();
    this.batchSize = subscription.getBatchSubscriptionPolicy().getBatchSize();
    this.byteBuffer = buffer;
    this.additionalHeaders = subscription.getHeaders();
    this.topic = subscription.getQualifiedTopicName();
    this.subscription = subscription.getQualifiedName();
    this.hasSubscriptionIdentityHeaders = subscription.isSubscriptionIdentityHeadersEnabled();
  }

  @Override
  public boolean isFull() {
    return elements >= batchSize || byteBuffer.remaining() < 2;
  }

  @Override
  public void append(byte[] data, MessageMetadata metadata) {
    checkState(!closed, "Batch already closed.");
    if (!canFit(data)) {
      throw new BufferOverflowException();
    }
    if (isEmpty()) {
      batchStart = clock.millis();
    }

    byteBuffer.put((byte) (isEmpty() ? '[' : ',')).put(data);
    this.metadata.add(metadata);
    elements++;
  }

  @Override
  public boolean canFit(byte[] data) {
    return byteBuffer.remaining() >= requiredFreeSpace(data);
  }

  private int requiredFreeSpace(byte[] data) {
    return data.length + 2;
  }

  @Override
  public boolean isExpired() {
    return !isEmpty() && getLifetime() > maxBatchTime;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public ContentType getContentType() {
    return ContentType.JSON;
  }

  @Override
  public MessageBatch close() {
    if (!isEmpty()) {
      byteBuffer.put((byte) ']');
    }
    int position = byteBuffer.position();
    byteBuffer.position(0);
    byteBuffer.limit(position);
    this.closed = true;
    return this;
  }

  @Override
  public ByteBuffer getContent() {
    if (closed) {
      byteBuffer.position(0);
    }
    return byteBuffer;
  }

  @Override
  public List<SubscriptionPartitionOffset> getPartitionOffsets() {
    return metadata.stream()
        .map(
            m ->
                subscriptionPartitionOffset(
                    this.subscription,
                    new PartitionOffset(
                        KafkaTopicName.valueOf(m.getKafkaTopic()), m.getOffset(), m.getPartition()),
                    m.getPartitionAssignmentTerm()))
        .collect(Collectors.toList());
  }

  @Override
  public List<MessageMetadata> getMessagesMetadata() {
    return Collections.unmodifiableList(metadata);
  }

  @Override
  public List<Header> getAdditionalHeaders() {
    return Collections.unmodifiableList(additionalHeaders);
  }

  @Override
  public int getMessageCount() {
    return elements;
  }

  @Override
  public long getLifetime() {
    return clock.millis() - batchStart;
  }

  @Override
  public boolean isClosed() {
    return closed;
  }

  @Override
  public boolean isEmpty() {
    return elements == 0;
  }

  @Override
  public boolean isBiggerThanTotalCapacity(byte[] data) {
    return requiredFreeSpace(data) > getCapacity();
  }

  @Override
  public int getCapacity() {
    return byteBuffer.capacity();
  }

  @Override
  public int getSize() {
    if (closed) {
      return byteBuffer.limit();
    }
    return byteBuffer.position();
  }

  @Override
  public void incrementRetryCounter() {
    this.retryCounter++;
  }

  @Override
  public int getRetryCounter() {
    return retryCounter;
  }

  @Override
  public boolean hasSubscriptionIdentityHeaders() {
    return hasSubscriptionIdentityHeaders;
  }

  @Override
  public String getTopic() {
    return topic;
  }

  @Override
  public SubscriptionName getSubscription() {
    return subscription;
  }
}
