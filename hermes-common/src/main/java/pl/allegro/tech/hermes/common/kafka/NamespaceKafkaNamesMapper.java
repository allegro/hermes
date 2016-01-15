package pl.allegro.tech.hermes.common.kafka;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;

import java.util.function.Function;

public class NamespaceKafkaNamesMapper implements KafkaNamesMapper {

    private static final String SEPARATOR = "_";

    private final String namespace;

    public NamespaceKafkaNamesMapper(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public ConsumerGroupId toConsumerGroupId(Subscription subscription) {
        return toConsumerGroupId(subscription.getId());
    }

    @Override
    public ConsumerGroupId toConsumerGroupId(String subscriptionId) {
        return ConsumerGroupId.valueOf(appendNamespace(subscriptionId));
    }

    @Override
    public KafkaTopics toKafkaTopics(Topic topic) {
        return mapToKafkaTopic.andThen(appendNamespace).andThen(mapToKafkaTopics).apply(topic);
    }

    protected Function<Topic, KafkaTopic> mapToKafkaTopic = it ->
            new KafkaTopic(KafkaTopicName.valueOf(it.getQualifiedName()), it.getContentType());

    protected Function<KafkaTopic, KafkaTopic> appendNamespace = it ->
            new KafkaTopic(KafkaTopicName.valueOf(appendNamespace(it.name().asString())), it.contentType());

    protected Function<KafkaTopic, KafkaTopics> mapToKafkaTopics = it -> new KafkaTopics(it);

    private String appendNamespace(String name) {
        return namespace.isEmpty()? name : namespace + SEPARATOR + name;
    }
}
