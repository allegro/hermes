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

    public KafkaTopics toKafkaTopics(Topic topic) {
        KafkaTopic primary = new KafkaTopic(toKafkaTopicName(topic, topic.getContentType()), topic.getContentType());
        if (topic.wasMigratedFromJsonType()) {
            KafkaTopic secondary = new KafkaTopic(toKafkaTopicName(topic, Topic.ContentType.JSON), Topic.ContentType.JSON);
            return new KafkaTopics(primary, secondary);
        }
        return new KafkaTopics(primary);
    }

    private KafkaTopicName toKafkaTopicName(Topic topic, Topic.ContentType contentType) {
        return KafkaTopicName.valueOf(namespaced(topic.getName().qualifiedName() + topicNameSuffix(contentType)));
    }

    private String namespaced(String name) {
        if (namespace.isEmpty()) {
            return name;
        }

        return namespace + SEPARATOR + name;
    }

    private String topicNameSuffix(Topic.ContentType contentType) {
        switch (contentType) {
            case JSON:
                return "";
            case AVRO:
                return "_avro";
        }

        throw new IllegalStateException("unknown content type '" + contentType + "'");
    }

}
