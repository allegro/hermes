package pl.allegro.tech.hermes.domain.topic.schema;

import pl.allegro.tech.hermes.api.Topic;

public class TopicWithSchema<T> {

    private final Topic topic;
    private final T schema;

    TopicWithSchema(Topic topic, T schema) {
        this.topic = topic;
        this.schema = schema;
    }

    public Topic getTopic() {
        return topic;
    }

    public T getSchema() {
        return schema;
    }
}
