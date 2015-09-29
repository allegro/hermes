package pl.allegro.tech.hermes.common.kafka;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;

import static com.google.common.base.Preconditions.checkNotNull;

public class KafkaNamesMapper {

    private static final String SEPARATOR = "_";

    private final String namespace;

    public KafkaNamesMapper(String namespace) {
        this.namespace = checkNotNull(namespace);
    }

    public ConsumerGroupId toConsumerGroupId(Subscription subscription) {
        return toConsumerGroupId(subscription.getId());
    }

    public ConsumerGroupId toConsumerGroupId(String subscriptionId) {
        return ConsumerGroupId.valueOf(namespaced(subscriptionId));
    }

    public KafkaTopic toKafkaTopicName(Topic topic) {
        return new KafkaTopic(namespaced(topic.getName().qualifiedName()));
    }

    private String namespaced(String name) {
        if (namespace.isEmpty()) {
            return name;
        }

        return namespace + SEPARATOR + name;
    }

}
