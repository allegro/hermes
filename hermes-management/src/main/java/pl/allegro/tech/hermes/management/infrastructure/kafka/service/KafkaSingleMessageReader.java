package pl.allegro.tech.hermes.management.infrastructure.kafka.service;

import pl.allegro.tech.hermes.api.SchemaSource;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.message.converter.AvroToJsonConverter;
import pl.allegro.tech.hermes.common.message.wrapper.AvroMessageContentWrapper;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaSourceProvider;
import pl.allegro.tech.hermes.management.domain.topic.SingleMessageReader;

import java.nio.charset.Charset;

public class KafkaSingleMessageReader implements SingleMessageReader {
    private final KafkaRawMessageReader kafkaRawMessageReader;
    private final AvroMessageContentWrapper avroMessageContentWrapper;
    private final SchemaSourceProvider schemaSourceProvider;

    public KafkaSingleMessageReader(KafkaRawMessageReader kafkaRawMessageReader,
                                    AvroMessageContentWrapper avroMessageContentWrapper,
                                    SchemaSourceProvider schemaSourceProvider) {
        this.kafkaRawMessageReader = kafkaRawMessageReader;
        this.avroMessageContentWrapper = avroMessageContentWrapper;
        this.schemaSourceProvider = schemaSourceProvider;
    }

    @Override
    public String readMessage(Topic topic, int partition, long offset) {
        byte[] bytes = kafkaRawMessageReader.readMessage(topic.getQualifiedName(), partition, offset);
        if (topic.getContentType() == Topic.ContentType.AVRO) {
            bytes = convertAvroToJson(schemaSourceProvider.get(topic).get(), bytes);
        }
        return new String(bytes, Charset.forName("UTF-8"));
    }

    private byte[] convertAvroToJson(SchemaSource schema, byte[] bytes) {
        return AvroToJsonConverter.convert(bytes, avroMessageContentWrapper.getWrappedSchema(schema));
    }
}
