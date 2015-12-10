package pl.allegro.tech.hermes.common.kafka;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.Topic;

public class KafkaNamesMapperHolder implements KafkaNamesMapper {

    private KafkaNamesMapper kafkaNamespaceMapper;

    public KafkaNamesMapperHolder(KafkaNamesMapper kafkaNamespaceMapper) {
        this.kafkaNamespaceMapper = kafkaNamespaceMapper;
    }

    public KafkaNamesMapper getKafkaNamespaceMapper() {
        return kafkaNamespaceMapper;
    }

    public void setKafkaNamespaceMapper(KafkaNamesMapper kafkaNamespaceMapper) {
        this.kafkaNamespaceMapper = kafkaNamespaceMapper;
    }

    @Override
    public ConsumerGroupId toConsumerGroupId(Subscription subscription) {
        return kafkaNamespaceMapper.toConsumerGroupId(subscription);
    }

    @Override
    public ConsumerGroupId toConsumerGroupId(String subscriptionId) {
        return null;
    }

    @Override
    public KafkaTopics toKafkaTopics(Topic topic) {
        return null;
    }
}
