package pl.allegro.tech.hermes.management.domain.topic.schema;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.SchemaSource;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.domain.topic.schema.TopicFieldSchemaSourceProvider;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;

import static pl.allegro.tech.hermes.api.Topic.Builder.topic;

@Component
public class TopicFieldSchemaSourceRepository extends TopicFieldSchemaSourceProvider implements SchemaSourceRepository {

    private final TopicService topicService;

    @Autowired
    public TopicFieldSchemaSourceRepository(@Lazy TopicService topicService) {
        this.topicService = topicService;
    }

    @Override
    public void save(SchemaSource schemaSource, Topic topic) {
        topicService.updateTopic(topic().applyPatch(topic).withMessageSchema(schemaSource.value()).build());
    }

    @Override
    public void delete(Topic topic) {
        throw new UnsupportedOperationException("Not supported in this temporary implementation");
    }

}
