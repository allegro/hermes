package pl.allegro.tech.hermes.management.infrastructure.kafka.service;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.message.converter.AvroToJsonConverter;
import pl.allegro.tech.hermes.common.message.wrapper.AvroMessageContentWrapper;
import pl.allegro.tech.hermes.management.domain.topic.SingleMessageReader;

import java.nio.charset.Charset;

public class KafkaSingleMessageReader implements SingleMessageReader {
    private final KafkaRawMessageReader kafkaRawMessageReader;
    private final AvroMessageContentWrapper avroMessageContentWrapper;

    public KafkaSingleMessageReader(KafkaRawMessageReader kafkaRawMessageReader,
                                    AvroMessageContentWrapper avroMessageContentWrapper) {
        this.kafkaRawMessageReader = kafkaRawMessageReader;
        this.avroMessageContentWrapper = avroMessageContentWrapper;
    }

    @Override
    public String readMessage(Topic topic, int partition, long offset) {
        byte[] bytes = kafkaRawMessageReader.readMessage(topic.getQualifiedName(), partition, offset);
        if (topic.getContentType() == Topic.ContentType.AVRO) {
            bytes = convertAvroToJson(topic.getMessageSchema(), bytes);
        }
        return new String(bytes, Charset.forName("UTF-8"));
    }

    private byte[] convertAvroToJson(String schema, byte[] bytes) {
        return new AvroToJsonConverter(avroMessageContentWrapper.getWrappedSchema(schema)).convert(bytes);
    }
}
