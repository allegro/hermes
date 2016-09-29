package pl.allegro.tech.hermes.consumers.test;

import org.apache.avro.Schema;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Header;
import pl.allegro.tech.hermes.common.kafka.KafkaTopicName;
import pl.allegro.tech.hermes.common.kafka.offset.PartitionOffset;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.schema.CompiledSchema;
import pl.allegro.tech.hermes.schema.SchemaVersion;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.collect.ImmutableMap.of;

public final class MessageBuilder {

    public static final String TEST_MESSAGE_CONTENT = "Some test message";

    private String id;
    private String topic;
    private ContentType contentType;
    private long publishingTimestamp;
    private long readingTimestamp;
    private PartitionOffset partitionOffset;
    private byte[] content;
    private Map<String, String> externalMetadata;
    private List<Header> additionalHeaders;
    private Optional<CompiledSchema<Object>> schema = Optional.empty();

    private MessageBuilder() {
    }

    public static Message testMessage() {
        return MessageBuilder.withTestMessage().build();
    }

    public static MessageBuilder withTestMessage() {
        return new MessageBuilder()
                .withId("id")
                .withTopic("topicId")
                .withContent(TEST_MESSAGE_CONTENT, StandardCharsets.UTF_8)
                .withContentType(ContentType.JSON)
                .withPublishingTimestamp(123L)
                .withReadingTimestamp(123L)
                .withPartitionOffset(new PartitionOffset(KafkaTopicName.valueOf("kafka_topic"), 123, 1))
                .withExternalMetadata(of("Trace-Id", "traceId"))
                .withAdditionalHeaders(Collections.emptyList());
    }

    public Message build() {
        return new Message(id, topic, content, contentType, schema, publishingTimestamp,
                readingTimestamp, partitionOffset, externalMetadata, additionalHeaders);
    }

    public MessageBuilder withId(String id) {
        this.id = id;
        return this;
    }

    public MessageBuilder withSchema(Schema schema, int version) {
        this.schema = Optional.of(new CompiledSchema<Object>(schema, SchemaVersion.valueOf(version)));
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

    public MessageBuilder withPartitionOffset(String kafkaTopic, int partition, long offset) {
        this.partitionOffset = new PartitionOffset(KafkaTopicName.valueOf(kafkaTopic), offset, partition);
        return this;
    }

    public MessageBuilder withExternalMetadata(Map<String, String> externalMetadata) {
        this.externalMetadata = externalMetadata;
        return this;
    }

    public MessageBuilder withAdditionalHeaders(List<Header> additionalHeaders) {
        this.additionalHeaders = additionalHeaders;
        return this;
    }
}
