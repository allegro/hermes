package pl.allegro.tech.hermes.common.kafka;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;

public interface KafkaNamesMapper {

    ConsumerGroupId toConsumerGroupId(Subscription subscription);

    ConsumerGroupId toConsumerGroupId(String subscriptionId);

    KafkaTopics toKafkaTopics(Topic topic);
}
