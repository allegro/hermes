package pl.allegro.tech.hermes.consumers.consumer.batch;

import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.tracker.consumers.MessageMetadata;

import javax.annotation.concurrent.NotThreadSafe;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;

@NotThreadSafe
public class JsonMessageBatch implements MessageBatch {
    private final Clock clock;

    private final int maxBatchTime;
    private final int batchSize;

    private final String id;
    private final ByteBuffer byteBuffer;
    private final List<MessageMetadata> metadata = new ArrayList<>();

    private int elements = 0;
    private long batchStart;
    private boolean closed = false;

    public JsonMessageBatch(String id, ByteBuffer buffer, int size, int batchTime, Clock clock) {
        this.id = id;
        this.clock = clock;
        this.maxBatchTime = batchTime;
        this.batchSize = size;
        this.byteBuffer = buffer;
    }

    public JsonMessageBatch(String id, ByteBuffer buffer, Subscription subscription, Clock clock) {
        this(id, buffer,
                subscription.getBatchSubscriptionPolicy().getBatchSize(),
                subscription.getBatchSubscriptionPolicy().getBatchTime(),
                clock);
    }

    @Override
    public boolean isFull() {
        return elements >= batchSize || byteBuffer.remaining() < 2;
    }

    @Override
    public void append(byte[] data, MessageMetadata metadata) {
        checkState(!closed, "Batch already closed.");
        if (!canFit(data)) throw new BufferOverflowException();
        if (isEmpty()) batchStart = clock.millis();

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
        if (!isEmpty()) byteBuffer.put((byte)']');
        int position = byteBuffer.position();
        byteBuffer.position(0);
        byteBuffer.limit(position);
        this.closed = true;
        return this;
    }

    @Override
    public ByteBuffer getContent() {
        if (closed) byteBuffer.position(0);
        return byteBuffer;
    }

    @Override
    public List<PartitionOffset> getPartitionOffsets() {
        return metadata.stream()
                .map(m -> new PartitionOffset(KafkaTopicName.valueOf(m.getTopic()), m.getOffset(), m.getPartition()))
                .collect(Collectors.toList());
    }

    @Override
    public List<MessageMetadata> getMessagesMetadata() {
        return Collections.unmodifiableList(metadata);
    }

    @Override
    public int size() {
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
}