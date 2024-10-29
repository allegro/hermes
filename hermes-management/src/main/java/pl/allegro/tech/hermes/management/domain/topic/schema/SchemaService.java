package pl.allegro.tech.hermes.management.domain.topic.schema;

import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.api.TopicName.fromQualifiedName;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.RawSchema;
import pl.allegro.tech.hermes.api.RawSchemaWithMetadata;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.management.config.TopicProperties;
import pl.allegro.tech.hermes.management.infrastructure.schema.validator.SchemaValidator;
import pl.allegro.tech.hermes.management.infrastructure.schema.validator.SchemaValidatorProvider;
import pl.allegro.tech.hermes.schema.RawSchemaClient;
import pl.allegro.tech.hermes.schema.SchemaId;
import pl.allegro.tech.hermes.schema.SchemaVersion;

@Component
public class SchemaService {

  private final RawSchemaClient rawSchemaClient;
  private final SchemaValidatorProvider validatorProvider;
  private final TopicProperties topicProperties;

  private static final Logger logger = LoggerFactory.getLogger(SchemaService.class);

  @Autowired
  public SchemaService(
      RawSchemaClient rawSchemaClient,
      SchemaValidatorProvider validatorProvider,
      TopicProperties topicProperties) {
    this.rawSchemaClient = rawSchemaClient;
    this.validatorProvider = validatorProvider;
    this.topicProperties = topicProperties;
  }

  public Optional<RawSchema> getSchema(String qualifiedTopicName) {
    return rawSchemaClient
        .getLatestRawSchemaWithMetadata(fromQualifiedName(qualifiedTopicName))
        .map(RawSchemaWithMetadata::getSchema);
  }

  public Optional<RawSchema> getSchema(String qualifiedTopicName, SchemaVersion version) {
    return rawSchemaClient
        .getRawSchemaWithMetadata(fromQualifiedName(qualifiedTopicName), version)
        .map(RawSchemaWithMetadata::getSchema);
  }

  public Optional<RawSchema> getSchema(String qualifiedTopicName, SchemaId id) {
    return rawSchemaClient
        .getRawSchemaWithMetadata(fromQualifiedName(qualifiedTopicName), id)
        .map(RawSchemaWithMetadata::getSchema);
  }

  public void registerSchema(Topic topic, String schema) {
    boolean validate = AVRO.equals(topic.getContentType());
    registerSchema(topic, schema, validate);
  }

  public void registerSchema(Topic topic, String schema, boolean validate) {
    if (validate) {
      SchemaValidator validator = validatorProvider.provide(topic.getContentType());
      validator.check(schema);
    }
    rawSchemaClient.registerSchema(topic.getName(), RawSchema.valueOf(schema));
  }

  public void deleteAllSchemaVersions(String qualifiedTopicName) {
    if (!topicProperties.isRemoveSchema()) {
      throw new SchemaRemovalDisabledException();
    }
    logger.info("Removing all schema versions for topic: {}", qualifiedTopicName);
    long start = System.currentTimeMillis();
    rawSchemaClient.deleteAllSchemaVersions(fromQualifiedName(qualifiedTopicName));
    logger.info(
        "Removed all schema versions for topic: {} in {} ms",
        qualifiedTopicName,
        System.currentTimeMillis() - start);
  }

  public void validateSchema(Topic topic, String schema) {
    if (AVRO.equals(topic.getContentType())) {
      SchemaValidator validator = validatorProvider.provide(AVRO);
      validator.check(schema);
    }
    rawSchemaClient.validateSchema(topic.getName(), RawSchema.valueOf(schema));
  }
}
