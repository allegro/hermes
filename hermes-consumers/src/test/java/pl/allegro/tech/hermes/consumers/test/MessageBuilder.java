package pl.allegro.tech.hermes.consumers.test;


import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.consumers.consumer.Message;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public final class MessageBuilder {

    private String id;
    private String topic;
    private String traceId;
    private Topic.ContentType contentType;
    private long publishingTimestamp;
    private long readingTimestamp;
    private PartitionOffset partitionOffset;
    private byte[] content;

    private MessageBuilder() {
    }

    public static MessageBuilder withTestMessage() {

        return new MessageBuilder()
                .withId("id")
                .withTopic("topicId")
                .withTraceId("traceId")
                .withContent("aaaaaaaa", StandardCharsets.UTF_8)
                .withContentType(Topic.ContentType.JSON)
                .withPublishingTimestamp(123L)
                .withReadingTimestamp(123L)
                .withPartitionOffset(new PartitionOffset(KafkaTopicName.valueOf("kafka_topic"), 123, 1));
    }

    public Message build() {
        return new Message(id, topic, traceId, content, contentType, publishingTimestamp,
                readingTimestamp, partitionOffset);
    }

    public MessageBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public MessageBuilder withTopic(String topic) {
        this.topic = topic;
        return this;
    }

    public MessageBuilder withTraceId(String traceId) {
        this.traceId = traceId;
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

    public MessageBuilder withContentType(Topic.ContentType contentType) {
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
}
