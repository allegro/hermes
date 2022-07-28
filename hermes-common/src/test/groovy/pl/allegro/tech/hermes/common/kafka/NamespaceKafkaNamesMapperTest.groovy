package pl.allegro.tech.hermes.common.kafka

import pl.allegro.tech.hermes.api.ContentType
import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.api.TopicName
import spock.lang.Specification

import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic

class NamespaceKafkaNamesMapperTest extends Specification {

    def mapper = new NamespaceKafkaNamesMapper("namespace", "_")

    def "should create consumer group id from subscription"() {
        given:
        SubscriptionName subscriptionName = new SubscriptionName('subscription', TopicName.fromQualifiedName('pl.group.topic'))

        when:
        ConsumerGroupId consumerGroupId = mapper.toConsumerGroupId(subscriptionName)

        then:
        consumerGroupId.asString() == "namespace_pl.group_topic_subscription"
    }

    def "should create consumer group id from subscription id"() {
        given:
        SubscriptionName name = SubscriptionName.fromString('pl.group.topic$subscription')

        when:
        ConsumerGroupId consumerGroupId = mapper.toConsumerGroupId(name)

        then:
        consumerGroupId.asString() == "namespace_pl.group_topic_subscription"
    }

    def "should create KafkaTopic from Topic"() {
        given:
        def topic = topic("pl.group.topic").withContentType(ContentType.AVRO).build()

        when:
        KafkaTopics kafkaTopics = mapper.toKafkaTopics(topic)

        then:
        KafkaTopic primary = kafkaTopics.getPrimary()
        primary.name().asString() == "namespace_pl.group.topic"
        primary.contentType() == ContentType.AVRO

        !kafkaTopics.getSecondary().isPresent()
    }
}
