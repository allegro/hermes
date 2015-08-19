package pl.allegro.tech.hermes.common.kafka

import pl.allegro.tech.hermes.api.Subscription
import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.api.TopicName
import spock.lang.Specification
import spock.lang.Unroll

class KafkaNamesMapperTest extends Specification {

    @Unroll
    def "should map topic '#topicName' to kafka topic '#kafkaTopicName' for namespace '#namespace'"() {
        given:
        def mapper = new KafkaNamesMapper(namespace)

        expect:
        mapper.toKafkaTopicName(TopicName.fromQualifiedName(topicName)) == KafkaTopicName.valueOf(kafkaTopicName)

        where:
        namespace | topicName     | kafkaTopicName
        "ns"      | "group.topic" | "ns_group.topic"
        ""        | "group.topic" | "group.topic"
    }

    @Unroll
    def "should map subscription id '#subscriptionId' to consumer group '#consumerGroupId' for namespace '#namespace'"() {
        given:
        def mapper = new KafkaNamesMapper(namespace)
        def subscription = Stub(Subscription) {
            getId() >> subscriptionId
        }

        expect:
        mapper.toConsumerGroupId(subscription) == ConsumerGroupId.valueOf(consumerGroupId)

        where:
        namespace | subscriptionId     | consumerGroupId
        "ns"      | "subscription_id" | "ns_subscription_id"
        ""        | "subscription_id" | "subscription_id"
    }

}
