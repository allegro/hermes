package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.message.wrapper.CompositeMessageContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.UnsupportedContentTypeException;
import pl.allegro.tech.hermes.common.message.wrapper.UnwrappedMessageContent;

class BasicMessageContentReader implements MessageContentReader {

    private final CompositeMessageContentWrapper compositeMessageContentWrapper;
    private final KafkaHeaderExtractor kafkaHeaderExtractor;
    private final Topic topic;

    BasicMessageContentReader(CompositeMessageContentWrapper compositeMessageContentWrapper,
                              KafkaHeaderExtractor kafkaHeaderExtractor,
                              Topic topic) {
        this.compositeMessageContentWrapper = compositeMessageContentWrapper;
        this.kafkaHeaderExtractor = kafkaHeaderExtractor;
        this.topic = topic;
    }

    @Override
    public UnwrappedMessageContent read(ConsumerRecord<byte[], byte[]> message, ContentType contentType) {
        if (contentType == ContentType.AVRO) {
            Integer schemaVersion = kafkaHeaderExtractor.extractSchemaVersion(message.headers());
            Integer schemaId = kafkaHeaderExtractor.extractSchemaId(message.headers());
            return compositeMessageContentWrapper.unwrapAvro(message.value(), topic, schemaId, schemaVersion);
        } else if (contentType == ContentType.JSON) {
            return compositeMessageContentWrapper.unwrapJson(message.value());
        }
        throw new UnsupportedContentTypeException(topic);
    }
}
