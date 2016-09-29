package pl.allegro.tech.hermes.management.domain.topic.schema;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.SchemaSource;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.management.config.TopicProperties;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;
import pl.allegro.tech.hermes.management.infrastructure.schema.validator.SchemaValidator;
import pl.allegro.tech.hermes.management.infrastructure.schema.validator.SchemaValidatorProvider;
import pl.allegro.tech.hermes.schema.SchemaSourceClient;
import pl.allegro.tech.hermes.schema.SchemaVersion;

import java.util.Optional;

import static pl.allegro.tech.hermes.api.TopicName.fromQualifiedName;

@Component
public class SchemaSourceService {

    private final TopicService topicService;
    private final SchemaSourceClient schemaSourceClient;
    private final SchemaValidatorProvider validatorProvider;
    private final TopicProperties topicProperties;

    @Autowired
    public SchemaSourceService(TopicService topicService, SchemaSourceClient schemaSourceClient,
                               SchemaValidatorProvider validatorProvider, TopicProperties topicProperties) {
        this.topicService = topicService;
        this.schemaSourceClient = schemaSourceClient;
        this.validatorProvider = validatorProvider;
        this.topicProperties = topicProperties;
    }

    public Optional<SchemaSource> getSchemaSource(String qualifiedTopicName) {
        Topic topic = findTopic(qualifiedTopicName);
        return schemaSourceClient.getLatestSchemaSource(topic.getName());
    }

    public void registerSchemaSource(String qualifiedTopicName, String schema, boolean validate) {
        Topic topic = findTopic(qualifiedTopicName);
        if (validate) {
            SchemaValidator validator = validatorProvider.provide(topic.getContentType());
            validator.check(schema);
        }
        schemaSourceClient.registerSchemaSource(topic.getName(), SchemaSource.valueOf(schema));
    }

    public Optional<SchemaSource> getSchemaSource(String qualifiedTopicName, SchemaVersion version) {
        Topic topic = findTopic(qualifiedTopicName);
        return schemaSourceClient.getSchemaSource(topic.getName(), version);
    }

    public void deleteAllSchemaSources(String qualifiedTopicName) {
        if (!topicProperties.isRemoveSchema()) {
            throw new SchemaRemovalDisabledException();
        }
        TopicName topicName = TopicName.fromQualifiedName(qualifiedTopicName);
        schemaSourceClient.deleteAllSchemaSources(topicName);
    }

    private Topic findTopic(String qualifiedTopicName) {
        return topicService.getTopicDetails(fromQualifiedName(qualifiedTopicName));
    }
}
