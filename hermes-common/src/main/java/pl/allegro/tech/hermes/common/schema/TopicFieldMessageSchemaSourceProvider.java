package pl.allegro.tech.hermes.common.schema;

import pl.allegro.tech.hermes.api.Topic;

public class TopicFieldMessageSchemaSourceProvider implements MessageSchemaSourceProvider {

    @Override
    public String get(Topic topic) {
        return topic.getMessageSchema();
    }

}
