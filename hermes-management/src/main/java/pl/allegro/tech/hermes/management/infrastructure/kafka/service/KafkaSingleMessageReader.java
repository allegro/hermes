package pl.allegro.tech.hermes.management.infrastructure.kafka.service;

import java.nio.charset.StandardCharsets;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.KafkaTopic;
import pl.allegro.tech.hermes.management.domain.topic.SingleMessageReader;
import pl.allegro.tech.hermes.schema.SchemaRepository;
import tech.allegro.schema.json2avro.converter.JsonAvroConverter;

public class KafkaSingleMessageReader implements SingleMessageReader {
  private final KafkaRawMessageReader kafkaRawMessageReader;
  private final SchemaRepository schemaRepository;
  private final JsonAvroConverter converter;

  public KafkaSingleMessageReader(
      KafkaRawMessageReader kafkaRawMessageReader,
      SchemaRepository schemaRepository,
      JsonAvroConverter converter) {
    this.kafkaRawMessageReader = kafkaRawMessageReader;
    this.schemaRepository = schemaRepository;
    this.converter = converter;
  }

  @Override
  public String readMessageAsJson(Topic topic, KafkaTopic kafkaTopic, int partition, long offset) {
    byte[] bytes = kafkaRawMessageReader.readMessage(kafkaTopic, partition, offset);
    if (topic.getContentType() == ContentType.AVRO) {
      bytes = convertAvroToJson(topic, bytes);
    }
    return new String(bytes, StandardCharsets.UTF_8);
  }

  private byte[] convertAvroToJson(Topic topic, byte[] bytes) {
    return converter.convertToJson(bytes, schemaRepository.getLatestAvroSchema(topic).getSchema());
  }
}
