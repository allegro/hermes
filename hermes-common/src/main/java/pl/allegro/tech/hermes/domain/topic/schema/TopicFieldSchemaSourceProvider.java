package pl.allegro.tech.hermes.domain.topic.schema;

import pl.allegro.tech.hermes.api.SchemaSource;
import pl.allegro.tech.hermes.api.Topic;

public class TopicFieldSchemaSourceProvider implements SchemaSourceProvider {

    @Override
    public SchemaSource get(Topic topic) {
        return SchemaSource.valueOf(topic.getMessageSchema());
    }

}
