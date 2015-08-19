package pl.allegro.tech.hermes.common.kafka;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;

import static com.google.common.base.Preconditions.checkNotNull;

public class KafkaNamesMapper {

    private static final String SEPARATOR = "_";

    private final String namespace;

    public KafkaNamesMapper(String namespace) {
        this.namespace = checkNotNull(namespace);
    }

    public ConsumerGroupId toConsumerGroupId(Subscription subscription) {
        return ConsumerGroupId.valueOf(namespaced(subscription.getId()));
    }

    public KafkaTopicName toKafkaTopicName(Topic topic) {
        return toKafkaTopicName(topic.getName());
    }

    public KafkaTopicName toKafkaTopicName(TopicName topicName) {
        return KafkaTopicName.valueOf(namespaced(topicName.qualifiedName()));
    }

    private String namespaced(String name) {
        if (namespace.isEmpty()) {
            return name;
        }

        return namespace + SEPARATOR + name;
    }

}
