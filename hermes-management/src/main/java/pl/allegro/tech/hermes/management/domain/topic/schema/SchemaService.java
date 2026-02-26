package pl.allegro.tech.hermes.management.domain.topic.schema;

import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.api.TopicName.fromQualifiedName;
import static pl.allegro.tech.hermes.common.logging.LoggingFields.TOPIC_NAME;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.RawSchema;
import pl.allegro.tech.hermes.api.RawSchemaWithMetadata;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.management.infrastructure.schema.validator.SchemaValidator;
import pl.allegro.tech.hermes.management.infrastructure.schema.validator.SchemaValidatorProvider;
import pl.allegro.tech.hermes.schema.RawSchemaClient;
import pl.allegro.tech.hermes.schema.SchemaId;
import pl.allegro.tech.hermes.schema.SchemaVersion;

public class SchemaService {

  private final RawSchemaClient rawSchemaClient;
  private final SchemaValidatorProvider validatorProvider;
  private final boolean removeSchemaEnabled;

  private static final Logger logger = LoggerFactory.getLogger(SchemaService.class);

  public SchemaService(
      RawSchemaClient rawSchemaClient,
      SchemaValidatorProvider validatorProvider,
      boolean removeSchemaEnabled) {
    this.rawSchemaClient = rawSchemaClient;
    this.validatorProvider = validatorProvider;
    this.removeSchemaEnabled = removeSchemaEnabled;
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
    if (!removeSchemaEnabled) {
      throw new SchemaRemovalDisabledException();
    }
    logger
        .atInfo()
        .addKeyValue(TOPIC_NAME, qualifiedTopicName)
        .log("Removing all schema versions for topic: {}", qualifiedTopicName);
    long start = System.currentTimeMillis();
    rawSchemaClient.deleteAllSchemaVersions(fromQualifiedName(qualifiedTopicName));
    logger
        .atInfo()
        .addKeyValue(TOPIC_NAME, qualifiedTopicName)
        .log(
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
