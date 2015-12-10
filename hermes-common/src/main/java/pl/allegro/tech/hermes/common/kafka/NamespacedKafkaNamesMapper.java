package pl.allegro.tech.hermes.common.kafka;

import pl.allegro.tech.hermes.api.Subscription;

public abstract class NamespacedKafkaNamesMapper implements KafkaNamesMapper {

    private static final String SEPARATOR = "_";

    private final String namespace;

    public NamespacedKafkaNamesMapper(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public ConsumerGroupId toConsumerGroupId(Subscription subscription) {
        return toConsumerGroupId(subscription.getId());
    }

    @Override
    public ConsumerGroupId toConsumerGroupId(String subscriptionId) {
        return ConsumerGroupId.valueOf(namespaced(subscriptionId));
    }


    protected KafkaTopicName appendNamespace(String topicName) {
        return KafkaTopicName.valueOf(namespaced(topicName));
    }

    protected String namespaced(String name) {
        if (namespace.isEmpty()) {
            return name;
        }

        return namespace + SEPARATOR + name;
    }


}
