package pl.allegro.tech.hermes.management.domain.topic.schema;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.RawSchema;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.management.config.TopicProperties;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;
import pl.allegro.tech.hermes.management.infrastructure.schema.validator.SchemaValidator;
import pl.allegro.tech.hermes.management.infrastructure.schema.validator.SchemaValidatorProvider;
import pl.allegro.tech.hermes.schema.RawSchemaClient;
import pl.allegro.tech.hermes.schema.SchemaVersion;

import java.util.Optional;

import static pl.allegro.tech.hermes.api.TopicName.fromQualifiedName;

@Component
public class SchemaService {

    private final TopicService topicService;
    private final RawSchemaClient rawSchemaClient;
    private final SchemaValidatorProvider validatorProvider;
    private final TopicProperties topicProperties;

    @Autowired
    public SchemaService(TopicService topicService, RawSchemaClient rawSchemaClient,
                         SchemaValidatorProvider validatorProvider, TopicProperties topicProperties) {
        this.topicService = topicService;
        this.rawSchemaClient = rawSchemaClient;
        this.validatorProvider = validatorProvider;
        this.topicProperties = topicProperties;
    }

    public Optional<RawSchema> getSchema(String qualifiedTopicName) {
        Topic topic = findTopic(qualifiedTopicName);
        return rawSchemaClient.getLatestSchema(topic.getName());
    }

    public void registerSchema(String qualifiedTopicName, String schema, boolean validate) {
        Topic topic = findTopic(qualifiedTopicName);
        if (validate) {
            SchemaValidator validator = validatorProvider.provide(topic.getContentType());
            validator.check(schema);
        }
        rawSchemaClient.registerSchema(topic.getName(), RawSchema.valueOf(schema));
    }

    public Optional<RawSchema> getSchema(String qualifiedTopicName, SchemaVersion version) {
        Topic topic = findTopic(qualifiedTopicName);
        return rawSchemaClient.getSchema(topic.getName(), version);
    }

    public void deleteAllSchemaVersions(String qualifiedTopicName) {
        if (!topicProperties.isRemoveSchema()) {
            throw new SchemaRemovalDisabledException();
        }
        TopicName topicName = TopicName.fromQualifiedName(qualifiedTopicName);
        rawSchemaClient.deleteAllSchemaVersions(topicName);
    }

    private Topic findTopic(String qualifiedTopicName) {
        return topicService.getTopicDetails(fromQualifiedName(qualifiedTopicName));
    }
}
