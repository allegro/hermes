package pl.allegro.tech.hermes.domain.topic.schema;

import pl.allegro.tech.hermes.api.Topic;

public class TopicFieldSchemaSourceProvider implements SchemaSourceProvider {

    @Override
    public String get(Topic topic) {
        return topic.getMessageSchema();
    }

}
