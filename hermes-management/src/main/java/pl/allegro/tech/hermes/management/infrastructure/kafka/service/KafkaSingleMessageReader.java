package pl.allegro.tech.hermes.management.infrastructure.kafka.service;

import org.apache.avro.Schema;
import pl.allegro.tech.common.avro.JsonAvroConverter;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.KafkaTopic;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository;
import pl.allegro.tech.hermes.management.domain.topic.SingleMessageReader;

import java.nio.charset.Charset;

public class KafkaSingleMessageReader implements SingleMessageReader {
    private final KafkaRawMessageReader kafkaRawMessageReader;
    private final SchemaRepository<Schema> avroSchemaRepository;
    private JsonAvroConverter converter;

    public KafkaSingleMessageReader(KafkaRawMessageReader kafkaRawMessageReader,
                                    SchemaRepository<Schema> avroSchemaRepository,
                                    JsonAvroConverter converter) {
        this.kafkaRawMessageReader = kafkaRawMessageReader;
        this.avroSchemaRepository = avroSchemaRepository;
        this.converter = converter;
    }

    @Override
    public String readMessageAsJson(Topic topic, KafkaTopic kafkaTopic, int partition, long offset) {
        byte[] bytes = kafkaRawMessageReader.readMessage(kafkaTopic, partition, offset);
        if (topic.getContentType() == ContentType.AVRO) {
            bytes = convertAvroToJson(avroSchemaRepository.getSchema(topic), bytes);
        }
        return new String(bytes, Charset.forName("UTF-8"));
    }

    private byte[] convertAvroToJson(Schema schema, byte[] bytes) {
        return converter.convertToJson(bytes, schema);
    }
}
