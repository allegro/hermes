package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.message.wrapper.CompositeMessageContentWrapper;
import pl.allegro.tech.hermes.common.message.wrapper.UnsupportedContentTypeException;
import pl.allegro.tech.hermes.common.message.wrapper.UnwrappedMessageContent;
import pl.allegro.tech.hermes.consumers.consumer.receiver.RetryableReceiverError;
import pl.allegro.tech.hermes.schema.SchemaExistenceEnsurer;
import pl.allegro.tech.hermes.schema.SchemaId;
import pl.allegro.tech.hermes.schema.SchemaVersion;

class BasicMessageContentReader implements MessageContentReader {

  private final CompositeMessageContentWrapper compositeMessageContentWrapper;
  private final KafkaHeaderExtractor kafkaHeaderExtractor;
  private final Topic topic;
  private final SchemaExistenceEnsurer schemaExistenceEnsurer;

  BasicMessageContentReader(
      CompositeMessageContentWrapper compositeMessageContentWrapper,
      KafkaHeaderExtractor kafkaHeaderExtractor,
      Topic topic,
      SchemaExistenceEnsurer schemaExistenceEnsurer) {
    this.compositeMessageContentWrapper = compositeMessageContentWrapper;
    this.kafkaHeaderExtractor = kafkaHeaderExtractor;
    this.topic = topic;
    this.schemaExistenceEnsurer = schemaExistenceEnsurer;
  }

  @Override
  public UnwrappedMessageContent read(
      ConsumerRecord<byte[], byte[]> message, ContentType contentType) {
    if (contentType == ContentType.AVRO) {
      Integer schemaVersion = kafkaHeaderExtractor.extractSchemaVersion(message.headers());
      Integer schemaId = kafkaHeaderExtractor.extractSchemaId(message.headers());
      ensureExistence(schemaVersion, schemaId);
      return compositeMessageContentWrapper.unwrapAvro(
          message.value(), topic, schemaId, schemaVersion);
    } else if (contentType == ContentType.JSON) {
      return compositeMessageContentWrapper.unwrapJson(message.value());
    }
    throw new UnsupportedContentTypeException(topic);
  }

  private void ensureExistence(Integer schemaVersion, Integer schemaId) {
    try {
      if (schemaVersion != null) {
        schemaExistenceEnsurer.ensureSchemaExists(topic, SchemaVersion.valueOf(schemaVersion));
      }
      if (schemaId != null) {
        schemaExistenceEnsurer.ensureSchemaExists(topic, SchemaId.valueOf(schemaId));
      }
    } catch (SchemaExistenceEnsurer.SchemaNotLoaded ex) {
      throw new RetryableReceiverError("Requested schema not present yet...", ex);
    }
  }
}
