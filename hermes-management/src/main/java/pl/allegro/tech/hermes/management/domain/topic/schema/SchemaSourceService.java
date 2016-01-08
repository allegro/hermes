package pl.allegro.tech.hermes.management.domain.topic.schema;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.SchemaSource;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;
import pl.allegro.tech.hermes.management.infrastructure.schema.validator.SchemaValidator;
import pl.allegro.tech.hermes.management.infrastructure.schema.validator.SchemaValidatorProvider;

import java.util.Optional;

import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.api.TopicName.fromQualifiedName;

@Component
public class SchemaSourceService {

    private final TopicService topicService;
    private final SchemaSourceRepository schemaSourceRepository;
    private final SchemaValidatorProvider validatorProvider;

    @Autowired
    public SchemaSourceService(TopicService topicService, SchemaSourceRepository schemaSourceRepository, SchemaValidatorProvider validatorProvider) {
        this.topicService = topicService;
        this.schemaSourceRepository = schemaSourceRepository;
        this.validatorProvider = validatorProvider;
    }

    public Optional<SchemaSource> getSchemaSource(String qualifiedTopicName) {
        Topic topic = findTopic(qualifiedTopicName);
        return schemaSourceRepository.get(topic);
    }

    public void saveSchemaSource(String qualifiedTopicName, String schema, boolean validate) {
        Topic topic = findTopic(qualifiedTopicName);
        if (validate) {
            SchemaValidator validator = validatorProvider.provide(topic.getContentType());
            validator.check(schema);
        }
        schemaSourceRepository.save(SchemaSource.valueOf(schema), topic);
    }

    public void deleteSchemaSource(String qualifiedTopicName) {
        Topic topic = findTopic(qualifiedTopicName);
        if (topic.getContentType() == AVRO) {
            throw new AvroSchemaRemovalDisabledException("Topic " + qualifiedTopicName + " has Avro content-type, schema removal is disabled");
        }
        schemaSourceRepository.delete(topic);
    }

    private Topic findTopic(String qualifiedTopicName) {
        return topicService.getTopicDetails(fromQualifiedName(qualifiedTopicName));
    }
}
