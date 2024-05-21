package pl.allegro.tech.hermes.management.domain.topic.schema;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.RawSchema;
import pl.allegro.tech.hermes.api.RawSchemaWithMetadata;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.management.config.TopicProperties;
import pl.allegro.tech.hermes.management.infrastructure.schema.validator.SchemaValidator;
import pl.allegro.tech.hermes.management.infrastructure.schema.validator.SchemaValidatorProvider;
import pl.allegro.tech.hermes.schema.RawSchemaAdminClient;
import pl.allegro.tech.hermes.schema.SchemaId;
import pl.allegro.tech.hermes.schema.SchemaVersion;

import java.util.Optional;

import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.api.TopicName.fromQualifiedName;

@Component
public class SchemaService {

    private final RawSchemaAdminClient rawSchemaAdminClient;
    private final SchemaValidatorProvider validatorProvider;
    private final TopicProperties topicProperties;

    @Autowired
    public SchemaService(RawSchemaAdminClient rawSchemaAdminClient,
                         SchemaValidatorProvider validatorProvider,
                         TopicProperties topicProperties) {
        this.rawSchemaAdminClient = rawSchemaAdminClient;
        this.validatorProvider = validatorProvider;
        this.topicProperties = topicProperties;
    }

    public Optional<RawSchema> getSchema(String qualifiedTopicName) {
        return rawSchemaAdminClient
            .getLatestRawSchemaWithMetadata(fromQualifiedName(qualifiedTopicName))
            .map(RawSchemaWithMetadata::getSchema);
    }

    public Optional<RawSchema> getSchema(String qualifiedTopicName, SchemaVersion version) {
        return rawSchemaAdminClient
                .getRawSchemaWithMetadata(fromQualifiedName(qualifiedTopicName), version)
                .map(RawSchemaWithMetadata::getSchema);
    }

    public Optional<RawSchema> getSchema(String qualifiedTopicName, SchemaId id) {
        return rawSchemaAdminClient
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
        rawSchemaAdminClient.registerSchema(topic.getName(), RawSchema.valueOf(schema));
    }

    public void deleteAllSchemaVersions(String qualifiedTopicName) {
        if (!topicProperties.isRemoveSchema()) {
            throw new SchemaRemovalDisabledException();
        }
        rawSchemaAdminClient.deleteAllSchemaVersions(fromQualifiedName(qualifiedTopicName));
    }

    public void validateSchema(Topic topic, String schema) {
        if (AVRO.equals(topic.getContentType())) {
            SchemaValidator validator = validatorProvider.provide(AVRO);
            validator.check(schema);
        }
        rawSchemaAdminClient.validateSchema(topic.getName(), RawSchema.valueOf(schema));
    }
}
