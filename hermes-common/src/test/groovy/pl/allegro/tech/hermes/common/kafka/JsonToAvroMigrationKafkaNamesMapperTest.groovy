package pl.allegro.tech.hermes.common.kafka

import pl.allegro.tech.hermes.api.ContentType
import pl.allegro.tech.hermes.api.SubscriptionName
import spock.lang.Specification
import spock.lang.Unroll

import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic

class JsonToAvroMigrationKafkaNamesMapperTest extends Specification {

    def namespaceSeparator = "_"

    @Unroll
    def "should map topic '#topicName' to kafka topic '#kafkaTopicName' for namespace '#namespace'"() {
        given:
        def mapper = new JsonToAvroMigrationKafkaNamesMapper(namespace, namespaceSeparator)

        expect:
        mapper.toKafkaTopics(topic(topicName).build()).primary.name() == KafkaTopicName.valueOf(kafkaTopicName)

        where:
        namespace | topicName     | kafkaTopicName
        "ns"      | "group.topic" | "ns_group.topic"
        ""        | "group.topic" | "group.topic"
    }

    @Unroll
    def "should map subscription name '#subscriptionName' to consumer group '#consumerGroupId' for namespace '#namespace'"() {
        given:
        def mapper = new JsonToAvroMigrationKafkaNamesMapper(namespace, namespaceSeparator)

        expect:
        mapper.toConsumerGroupId(subscriptionName) == ConsumerGroupId.valueOf(consumerGroupId)

        where:
        namespace | subscriptionName                                         | consumerGroupId
        "ns"      | SubscriptionName.fromString('group.topic$subscription')  | "ns_group_topic_subscription"
        ""        | SubscriptionName.fromString('group.topic$subscription')  | "group_topic_subscription"
    }

    def "should append '_avro' suffix for topics of type AVRO"() {
        given:
        def mapper = new JsonToAvroMigrationKafkaNamesMapper("", namespaceSeparator)
        def avroTopic = topic("group", "topic").withContentType(ContentType.AVRO).build()

        expect:
        mapper.toKafkaTopics(avroTopic).primary.name() == KafkaTopicName.valueOf("group.topic_avro")
    }

    def "should map to topics with secondary json topic for topics migrated from json to avro"() {
        given:
        def mapper = new JsonToAvroMigrationKafkaNamesMapper("", namespaceSeparator)
        def migratedTopic = topic("group", "topic").withContentType(ContentType.AVRO).migratedFromJsonType().build()

        when:
        def topics = mapper.toKafkaTopics(migratedTopic)

        then:
        topics.primary.contentType() == ContentType.AVRO
        topics.secondary.present
        topics.secondary.get().contentType() == ContentType.JSON
    }

}
