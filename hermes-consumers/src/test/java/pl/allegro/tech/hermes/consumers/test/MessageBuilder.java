package pl.allegro.tech.hermes.consumers.test;


import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.consumers.consumer.Message;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static com.google.common.collect.ImmutableMap.of;

public final class MessageBuilder {

    private String id;
    private String topic;
    private ContentType contentType;
    private long publishingTimestamp;
    private long readingTimestamp;
    private PartitionOffset partitionOffset;
    private byte[] content;
    private Map<String, String> externalMetadata;

    private MessageBuilder() {
    }

    public static MessageBuilder withTestMessage() {

        return new MessageBuilder()
                .withId("id")
                .withTopic("topicId")
                .withContent("aaaaaaaa", StandardCharsets.UTF_8)
                .withContentType(ContentType.JSON)
                .withPublishingTimestamp(123L)
                .withReadingTimestamp(123L)
                .withPartitionOffset(new PartitionOffset(KafkaTopicName.valueOf("kafka_topic"), 123, 1))
                .withExternalMetadata(of("Trace-Id", "traceId"));
    }

    public Message build() {
        return new Message(id, topic, content, contentType, publishingTimestamp,
                readingTimestamp, partitionOffset, externalMetadata);
    }

    public MessageBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public MessageBuilder withTopic(String topic) {
        this.topic = topic;
        return this;
    }

    public MessageBuilder withContent(String content, Charset charset) {
        this.content = content.getBytes(charset);
        return this;
    }

    public MessageBuilder withContent(byte[] content) {
        this.content = content;
        return this;
    }

    public MessageBuilder withContentType(ContentType contentType) {
        this.contentType = contentType;
        return this;
    }

    public MessageBuilder withPublishingTimestamp(long publishingTimestamp) {
        this.publishingTimestamp = publishingTimestamp;
        return this;
    }

    public MessageBuilder withReadingTimestamp(long readingTimestamp) {
        this.readingTimestamp = readingTimestamp;
        return this;
    }

    public MessageBuilder withPartitionOffset(PartitionOffset partitionOffset) {
        this.partitionOffset = partitionOffset;
        return this;
    }

    public MessageBuilder withExternalMetadata(Map<String, String> externalMetadata) {
        this.externalMetadata = externalMetadata;
        return this;
    }
}
