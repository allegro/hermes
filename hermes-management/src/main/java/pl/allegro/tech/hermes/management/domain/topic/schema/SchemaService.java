package pl.allegro.tech.hermes.management.domain.topic.schema;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.RawSchema;
import pl.allegro.tech.hermes.api.SchemaWithId;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.management.config.TopicProperties;
import pl.allegro.tech.hermes.management.infrastructure.schema.validator.SchemaValidator;
import pl.allegro.tech.hermes.management.infrastructure.schema.validator.SchemaValidatorProvider;
import pl.allegro.tech.hermes.schema.RawSchemaClient;
import pl.allegro.tech.hermes.schema.SchemaVersion;

import java.util.Optional;

import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.api.TopicName.fromQualifiedName;

@Component
public class SchemaService {

    private final RawSchemaClient rawSchemaClient;
    private final SchemaValidatorProvider validatorProvider;
    private final TopicProperties topicProperties;

    @Autowired
    public SchemaService(RawSchemaClient rawSchemaClient,
                         SchemaValidatorProvider validatorProvider,
                         TopicProperties topicProperties) {
        this.rawSchemaClient = rawSchemaClient;
        this.validatorProvider = validatorProvider;
        this.topicProperties = topicProperties;
    }

    public Optional<RawSchema> getSchema(String qualifiedTopicName) {
        return rawSchemaClient
            .getLatestSchemaWithId(fromQualifiedName(qualifiedTopicName))
            .map(SchemaWithId::getSchema);
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

    public Optional<RawSchema> getSchema(String qualifiedTopicName, SchemaVersion version) {
        return rawSchemaClient
            .getSchemaWithId(fromQualifiedName(qualifiedTopicName), version)
            .map(SchemaWithId::getSchema);
    }

    public void deleteAllSchemaVersions(String qualifiedTopicName) {
        if (!topicProperties.isRemoveSchema()) {
            throw new SchemaRemovalDisabledException();
        }
        rawSchemaClient.deleteAllSchemaVersions(fromQualifiedName(qualifiedTopicName));
    }

    public void validateSchema(Topic topic, String schema) {
        if (AVRO.equals(topic.getContentType())) {
            SchemaValidator validator = validatorProvider.provide(AVRO);
            validator.check(schema);
        }
        rawSchemaClient.validateSchema(topic.getName(), RawSchema.valueOf(schema));
    }
}
