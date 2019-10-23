package pl.allegro.tech.hermes.management.infrastructure.kafka.service

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.TopicPartition
import org.junit.Rule
import org.springframework.boot.test.rule.OutputCapture
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.spock.Testcontainers
import pl.allegro.tech.hermes.api.ContentType
import pl.allegro.tech.hermes.api.DeliveryType
import pl.allegro.tech.hermes.api.Subscription
import pl.allegro.tech.hermes.api.SubscriptionMode
import pl.allegro.tech.hermes.common.kafka.ConsumerGroupId
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper
import pl.allegro.tech.hermes.common.kafka.NamespaceKafkaNamesMapper
import pl.allegro.tech.hermes.management.domain.subscription.ConsumerGroupManager
import spock.lang.Shared
import spock.lang.Specification

import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG

@Testcontainers
class KafkaConsumerGroupManagerSpec extends Specification {

    @Shared
    KafkaContainer kafkaContainer = new KafkaContainer()

    @Rule
    OutputCapture output = new OutputCapture()

    KafkaNamesMapper namesMapper = new NamespaceKafkaNamesMapper("")
    AdminClient adminClient
    KafkaProducer<byte[], byte[]> producer
    ConsumerGroupManager consumerGroupManager
    ConsumerGroupId groupId

    def topic = "pl.allegro.test.Foo"
    def subscription = createTestSubscription("test-subscription")

    def setup() {
        kafkaContainer.start()
        kafkaContainer.waitingFor(Wait.forHealthcheck())

        Properties props = new Properties()
        props.put(BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.bootstrapServers)
        props.put(KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer")
        props.put(VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer")
        producer = new KafkaProducer<byte[], byte[]>(props)
        adminClient = AdminClient.create(props)

        adminClient.createTopics([new NewTopic(topic, 3, 1 as short)]).all().get()
        def describeTopicsResult = adminClient.describeTopics([topic]).all().get().values()
        // created topic has 3 partitions
        assert describeTopicsResult[0].name() == topic
        assert describeTopicsResult[0].partitions()
                .collect { it.partition() }
                .containsAll([0, 1, 2])

        groupId = namesMapper.toConsumerGroupId(subscription.qualifiedName)
        consumerGroupManager = new KafkaConsumerGroupManager(namesMapper, "primary", kafkaContainer.bootstrapServers)
    }

    def "should create consumer groups with offset equal to last topic offset"() {
        given:
        publishOnPartition(0, 10)
        publishOnPartition(1, 20)
        publishOnPartition(2, 15)

        when:
        consumerGroupManager.createConsumerGroup(subscription)

        then:
        def topicPartitionOffsets = adminClient
                .listConsumerGroupOffsets(groupId.asString())
                .partitionsToOffsetAndMetadata()
                .get()

        topicPartitionOffsets.get(new TopicPartition(topic, 0)).offset() == 10
        topicPartitionOffsets.get(new TopicPartition(topic, 1)).offset() == 20
        topicPartitionOffsets.get(new TopicPartition(topic, 2)).offset() == 15

        and:
        output.toString().contains 'Creating consumer group for subscription pl.allegro.test.Foo$test-subscription, cluster: primary'
        output.toString().contains 'Successfully created consumer groups for subscription pl.allegro.test.Foo$test-subscription, cluster: primary'
    }

    private def publishOnPartition(int partition, int messages) {
        for (int i = 0; i < messages; i++) {
            producer.send(new ProducerRecord<byte[], byte[]>(topic, partition, "key-$partition-$i".bytes, "val-$partition-$i".bytes))
        }
    }

    private def createTestSubscription(String subscriptionName) {
        Subscription.create(topic, subscriptionName, null, Subscription.State.PENDING, "test", [:], false, null, null,
                null, null, ContentType.JSON, DeliveryType.SERIAL, [], SubscriptionMode.ANYCAST, [], null, null, false, false
        )
    }
}
