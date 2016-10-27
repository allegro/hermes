package pl.allegro.tech.hermes.management.infrastructure.kafka.service.retransmit;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.KafkaTopic;
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.UnsupportedContentTypeException;
import pl.allegro.tech.hermes.common.message.wrapper.UnwrappedMessageContent;
import pl.allegro.tech.hermes.schema.SchemaRepository;
import pl.allegro.tech.hermes.management.infrastructure.kafka.service.KafkaRawMessageReader;

class KafkaTimestampExtractor {

    private final Topic topic;
    private final KafkaTopic kafkaTopic;
    private final int partition;
    private final KafkaRawMessageReader kafkaRawMessageReader;
    private final MessageContentWrapper messageContentWrapper;
    private final SchemaRepository schemaRepository;

    KafkaTimestampExtractor(Topic topic, KafkaTopic kafkaTopic, int partition, KafkaRawMessageReader kafkaRawMessageReader,
                            MessageContentWrapper messageContentWrapper, SchemaRepository schemaRepository) {

        this.topic = topic;
        this.kafkaTopic = kafkaTopic;
        this.partition = partition;
        this.kafkaRawMessageReader = kafkaRawMessageReader;
        this.messageContentWrapper = messageContentWrapper;
        this.schemaRepository = schemaRepository;
    }

    public long extract(Long offset) {
        byte[] message = kafkaRawMessageReader.readMessage(kafkaTopic, partition, offset);
        return unwrapContent(message).getMessageMetadata().getTimestamp();
    }

    private UnwrappedMessageContent unwrapContent(byte[] message) {
        switch (kafkaTopic.contentType()) {
            case AVRO:
                return messageContentWrapper.unwrapAvro(message, topic);
            case JSON:
                return messageContentWrapper.unwrapJson(message);
        }
        throw new UnsupportedContentTypeException(topic);
    }

}
