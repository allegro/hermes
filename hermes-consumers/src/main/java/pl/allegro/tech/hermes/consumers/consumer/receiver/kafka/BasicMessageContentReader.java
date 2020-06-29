package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.UnsupportedContentTypeException;
import pl.allegro.tech.hermes.common.message.wrapper.UnwrappedMessageContent;

class BasicMessageContentReader implements MessageContentReader {

    private final MessageContentWrapper messageContentWrapper;
    private final KafkaHeaderExtractor kafkaHeaderExtractor;
    private final Topic topic;

    BasicMessageContentReader(MessageContentWrapper messageContentWrapper,
                                     KafkaHeaderExtractor kafkaHeaderExtractor,
                                     Topic topic) {
        this.messageContentWrapper = messageContentWrapper;
        this.kafkaHeaderExtractor = kafkaHeaderExtractor;
        this.topic = topic;
    }

    @Override
    public UnwrappedMessageContent read(ConsumerRecord<byte[], byte[]> message, ContentType contentType) {
        if (contentType == ContentType.AVRO) {
            Integer schemaVersion = kafkaHeaderExtractor.extractSchemaVersion(message.headers());
            Integer schemaId = kafkaHeaderExtractor.extractSchemaId(message.headers());
            return messageContentWrapper.unwrapAvro(message.value(), topic, schemaId, schemaVersion);
        } else if (contentType == ContentType.JSON) {
            return messageContentWrapper.unwrapJson(message.value());
        }
        throw new UnsupportedContentTypeException(topic);
    }
}
