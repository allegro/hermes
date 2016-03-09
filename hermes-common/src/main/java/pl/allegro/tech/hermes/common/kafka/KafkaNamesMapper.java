package pl.allegro.tech.hermes.common.kafka;

import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.Topic;

public interface KafkaNamesMapper {

    ConsumerGroupId toConsumerGroupId(SubscriptionName subscription);

    ConsumerGroupId toConsumerGroupId(String subscriptionId);

    KafkaTopics toKafkaTopics(Topic topic);
}
