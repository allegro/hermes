package pl.allegro.tech.hermes.management.infrastructure.kafka.service;

import org.apache.avro.Schema;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper;
import pl.allegro.tech.hermes.common.message.converter.AvroToJsonConverter;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository;
import pl.allegro.tech.hermes.management.domain.topic.SingleMessageReader;

import java.nio.charset.Charset;

public class KafkaSingleMessageReader implements SingleMessageReader {
    private final KafkaRawMessageReader kafkaRawMessageReader;
    private final SchemaRepository<Schema> avroSchemaRepository;
    private final KafkaNamesMapper kafkaNamesMapper;

    public KafkaSingleMessageReader(KafkaRawMessageReader kafkaRawMessageReader,
                                    SchemaRepository<Schema> avroSchemaRepository, KafkaNamesMapper kafkaNamesMapper) {
        this.kafkaRawMessageReader = kafkaRawMessageReader;
        this.avroSchemaRepository = avroSchemaRepository;
        this.kafkaNamesMapper = kafkaNamesMapper;
    }

    @Override
    public String readMessage(Topic topic, int partition, long offset) {
        byte[] bytes = kafkaRawMessageReader.readMessage(kafkaNamesMapper.toKafkaTopicName(topic).asString(), partition, offset);
        if (topic.getContentType() == Topic.ContentType.AVRO) {
            bytes = convertAvroToJson(avroSchemaRepository.getSchema(topic), bytes);
        }
        return new String(bytes, Charset.forName("UTF-8"));
    }

    private byte[] convertAvroToJson(Schema schema, byte[] bytes) {
        return AvroToJsonConverter.convert(bytes, schema);
    }
}
