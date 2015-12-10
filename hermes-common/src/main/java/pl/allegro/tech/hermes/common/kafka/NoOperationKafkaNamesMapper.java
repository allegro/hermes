package pl.allegro.tech.hermes.common.kafka;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;

import java.util.function.Function;

public class NoOperationKafkaNamesMapper implements KafkaNamesMapper {

    @Override
    public ConsumerGroupId toConsumerGroupId(Subscription subscription) {
        return toConsumerGroupId(subscription.getId());
    }

    @Override
    public ConsumerGroupId toConsumerGroupId(String subscriptionId) {
        return ConsumerGroupId.valueOf(subscriptionId);
    }

    @Override
    public KafkaTopics toKafkaTopics(Topic topic) {
        return new KafkaTopics(toPrimaryKafkaTopic.apply(topic));
    }

    protected Function<Topic, KafkaTopic> toPrimaryKafkaTopic = it ->
            new KafkaTopic(KafkaTopicName.valueOf(it.getQualifiedName()), it.getContentType());
}
