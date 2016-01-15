package pl.allegro.tech.hermes.common.kafka

import pl.allegro.tech.hermes.api.ContentType
import pl.allegro.tech.hermes.api.Subscription
import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.api.Topic
import spock.lang.Specification


class NamespaceKafkaNamesMapperTest extends Specification {

    def mapper = new NamespaceKafkaNamesMapper("namespace")

    def "should create consumer group id from subscription"() {
        given:
        Subscription subscription = Subscription.fromSubscriptionName(SubscriptionName.fromString('pl.group.topic$subscription'))

        when:
        ConsumerGroupId consumerGroupId = mapper.toConsumerGroupId(subscription)

        then:
        consumerGroupId.asString() == "namespace_pl.group_topic_subscription"
    }

    def "should create consumer group id from subscription id"() {
        given:
        String subscriptionId = Subscription.fromSubscriptionName(SubscriptionName.fromString('pl.group.topic$subscription')).getId()

        when:
        ConsumerGroupId consumerGroupId = mapper.toConsumerGroupId(subscriptionId)

        then:
        consumerGroupId.asString() == "namespace_pl.group_topic_subscription"
    }

    def "should create KafkaTopic from Topic"() {
        given:
        def topic = Topic.Builder.topic().withName("pl.group.topic").withContentType(ContentType.AVRO).build()

        when:
        KafkaTopics kafkaTopics = mapper.toKafkaTopics(topic)

        then:
        KafkaTopic primary = kafkaTopics.getPrimary()
        primary.name().asString() == "namespace_pl.group.topic"
        primary.contentType() == ContentType.AVRO

        !kafkaTopics.getSecondary().isPresent()
    }
}
