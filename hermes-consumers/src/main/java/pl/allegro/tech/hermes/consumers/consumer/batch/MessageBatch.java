package pl.allegro.tech.hermes.consumers.consumer.batch;

import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.tracker.consumers.MessageMetadata;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public interface MessageBatch {

    default boolean isReadyForDelivery() {
        return isClosed() || isFull() || isExpired();
    }

    void append(byte[] data, MessageMetadata batchMessageMetadata) throws BufferOverflowException;

    boolean canFit(byte[] data);

    boolean isExpired();

    boolean isClosed();

    boolean isFull();

    String getId();

    ContentType getContentType();

    ByteBuffer getContent();

    List<PartitionOffset> getPartitionOffsets();

    List<MessageMetadata> getMessagesMetadata();

    Map<String, String> getAdditionalHeaders();

    long getLifetime();

    int size();

    MessageBatch close();

    boolean isEmpty();

    boolean isBiggerThanTotalCapacity(byte[] data);

    int getCapacity();

    void incrementRetryCounter();

    int getRetryCounter();
}
