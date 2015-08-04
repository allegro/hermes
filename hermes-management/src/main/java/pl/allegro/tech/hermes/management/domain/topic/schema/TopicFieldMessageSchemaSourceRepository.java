package pl.allegro.tech.hermes.management.domain.topic.schema;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.schema.TopicFieldMessageSchemaSourceProvider;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;

import static pl.allegro.tech.hermes.api.Topic.Builder.topic;

@Component
public class TopicFieldMessageSchemaSourceRepository extends TopicFieldMessageSchemaSourceProvider implements MessageSchemaSourceRepository {

    private final TopicService topicService;

    @Autowired
    public TopicFieldMessageSchemaSourceRepository(@Lazy TopicService topicService) {
        this.topicService = topicService;
    }

    @Override
    public void save(String schemaSource, Topic topic) {
        topicService.updateTopic(topic().applyPatch(topic).withMessageSchema(schemaSource).build());
    }

    @Override
    public void delete(Topic topic) {
        throw new UnsupportedOperationException("Not supported in this temporary implementation");
    }

}
