package pl.allegro.tech.hermes.management.infrastructure.schema;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import pl.allegro.tech.hermes.api.SchemaSource;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.domain.topic.schema.TopicFieldSchemaSourceProvider;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;
import pl.allegro.tech.hermes.management.domain.topic.schema.SchemaSourceRepository;

import static pl.allegro.tech.hermes.api.Topic.Builder.topic;

public class TopicFieldSchemaSourceRepository extends TopicFieldSchemaSourceProvider implements SchemaSourceRepository {

    private final TopicService topicService;

    @Autowired
    public TopicFieldSchemaSourceRepository(@Lazy TopicService topicService) {
        this.topicService = topicService;
    }

    @Override
    public void save(SchemaSource schemaSource, Topic topic) {
        saveValue(schemaSource.value(), topic);
    }

    @Override
    public void delete(Topic topic) {
        saveValue("", topic);
    }

    private void saveValue(String rawSchema, Topic topic) {
        topicService.updateTopic(topic().applyPatch(topic).withMessageSchema(rawSchema).build());
    }

}
