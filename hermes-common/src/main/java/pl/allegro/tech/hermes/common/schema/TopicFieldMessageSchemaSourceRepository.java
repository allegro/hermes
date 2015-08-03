package pl.allegro.tech.hermes.common.schema;

import pl.allegro.tech.hermes.api.Topic;

public class TopicFieldMessageSchemaSourceRepository implements MessageSchemaSourceRepository {

    @Override
    public String getSchemaSource(Topic topic) {
        return topic.getMessageSchema();
    }

}
