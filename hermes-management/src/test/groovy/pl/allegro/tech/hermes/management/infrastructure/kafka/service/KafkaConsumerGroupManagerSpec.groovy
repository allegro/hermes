package pl.allegro.tech.hermes.management.infrastructure.kafka.service

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
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
import pl.allegro.tech.hermes.api.Topic
import pl.allegro.tech.hermes.common.kafka.ConsumerGroupId
import pl.allegro.tech.hermes.common.kafka.JsonToAvroMigrationKafkaNamesMapper
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper
import pl.allegro.tech.hermes.management.config.kafka.KafkaProperties
import pl.allegro.tech.hermes.management.domain.subscription.ConsumerGroupManager
import pl.allegro.tech.hermes.test.helper.builder.TopicBuilder
import spock.lang.Shared
import spock.lang.Specification

import java.util.concurrent.CountDownLatch

import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG

@Testcontainers
class KafkaConsumerGroupManagerSpec extends Specification {

    @Rule
    OutputCapture output = new OutputCapture()

    @Shared
    KafkaContainer kafkaContainer = new KafkaContainer()

    @Shared
    String containerId

    @Shared
    AdminClient adminClient

    @Shared
    KafkaProducer<byte[], byte[]> producer

    @Shared
    KafkaNamesMapper kafkaNamesMapper = new JsonToAvroMigrationKafkaNamesMapper("")

    ConsumerGroupManager consumerGroupManager

    def setupSpec() {
        kafkaContainer.start()
        kafkaContainer.waitingFor(Wait.forHealthcheck())
        containerId = kafkaContainer.containerId

        Properties props = new Properties()
        props.put(BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.bootstrapServers)
        props.put(KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer")
        props.put(VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer")
        producer = new KafkaProducer<byte[], byte[]>(props)
        adminClient = AdminClient.create(props)
    }

    def setup() {
        consumerGroupManager = new KafkaConsumerGroupManager(kafkaNamesMapper, "primary", kafkaContainer.bootstrapServers, new KafkaProperties())
    }

    def "should create consumer group with offset equal to last topic offset"() {
        given:
        Topic topic = createAvroTopic("pl.allegro.test.Foo")
        Subscription subscription = createTestSubscription(topic, "test-subscription")
        String kafkaTopicName = kafkaNamesMapper.toKafkaTopics(topic).primary.name().asString()
        ConsumerGroupId consumerGroupId = kafkaNamesMapper.toConsumerGroupId(subscription.qualifiedName)
        createTopicInKafka(kafkaTopicName, 3)

        publishOnPartition(kafkaTopicName, 0, 10)
        publishOnPartition(kafkaTopicName, 1, 20)
        publishOnPartition(kafkaTopicName, 2, 15)

        when:
        consumerGroupManager.createConsumerGroup(topic, subscription)

        then:
        def topicPartitionOffsets = adminClient
                .listConsumerGroupOffsets(consumerGroupId.asString())
                .partitionsToOffsetAndMetadata()
                .get()

        topicPartitionOffsets.get(new TopicPartition(kafkaTopicName, 0)).offset() == 10
        topicPartitionOffsets.get(new TopicPartition(kafkaTopicName, 1)).offset() == 20
        topicPartitionOffsets.get(new TopicPartition(kafkaTopicName, 2)).offset() == 15

        and:
        output.toString().contains 'Creating consumer group for subscription pl.allegro.test.Foo$test-subscription, cluster: primary'
        output.toString().contains 'Successfully created consumer group for subscription pl.allegro.test.Foo$test-subscription, cluster: primary'
    }

    def "should override existing consumer group using offsets from the old consumer group"() {
        Topic topic = createAvroTopic("pl.allegro.test.overridingExistingConsumerGroup")
        Subscription subscription = createTestSubscription(topic, "test-subscription")
        String kafkaTopicName = kafkaNamesMapper.toKafkaTopics(topic).primary.name().asString()
        ConsumerGroupId consumerGroupId = kafkaNamesMapper.toConsumerGroupId(subscription.qualifiedName)
        createTopicInKafka(kafkaTopicName, 3)

        publishOnPartition(kafkaTopicName, 0, 10)
        publishOnPartition(kafkaTopicName, 1, 20)
        publishOnPartition(kafkaTopicName, 2, 15)

        when:
        consumerGroupManager.createConsumerGroup(topic, subscription)

        publishOnPartition(kafkaTopicName, 0, 1)
        publishOnPartition(kafkaTopicName, 1, 2)
        publishOnPartition(kafkaTopicName, 2, 3)

        consumerGroupManager.createConsumerGroup(topic, subscription)

        then:
        def topicPartitionOffsets = adminClient
                .listConsumerGroupOffsets(consumerGroupId.asString())
                .partitionsToOffsetAndMetadata()
                .get()

        topicPartitionOffsets.get(new TopicPartition(kafkaTopicName, 0)).offset() == 10
        topicPartitionOffsets.get(new TopicPartition(kafkaTopicName, 1)).offset() == 20
        topicPartitionOffsets.get(new TopicPartition(kafkaTopicName, 2)).offset() == 15
    }

    def "should not create consumer group and log exception in case of request timeout"() {
        given:
        kafkaContainer.dockerClient.pauseContainerCmd(containerId).exec()
        Topic topic = createAvroTopic("pl.allegro.test.ex")
        Subscription subscription = createTestSubscription(topic, "test-subscription")

        when:
        consumerGroupManager.createConsumerGroup(topic, subscription)

        then:
        noExceptionThrown()
        output.toString().contains 'Failed to create consumer group for subscription pl.allegro.test.ex$test-subscription, cluster: primary'
        output.toString().contains 'org.apache.kafka.common.errors.TimeoutException: Timeout expired while fetching topic metadata'

        cleanup:
        kafkaContainer.dockerClient.unpauseContainerCmd(containerId).exec()
    }

    private def publishOnPartition(String kafkaTopicName, int partition, int messages) {
        CountDownLatch countDownLatch = new CountDownLatch(messages)
        for (int i = 0; i < messages; i++) {
            producer.send(new ProducerRecord<byte[], byte[]>(kafkaTopicName, partition, "key-$partition-$i".bytes,
                    "val-$partition-$i".bytes), { RecordMetadata metadata, Exception exception ->
                if (exception == null) {
                    countDownLatch.countDown();
                } else {
                    throw new RuntimeException("Exception during message publishing", exception)
                }
            })
        }
        countDownLatch.await()
    }

    private def createTestSubscription(Topic topic, String subscriptionName) {
        Subscription.create(topic.getQualifiedName(), subscriptionName, null, Subscription.State.PENDING, "test", [:], false, null, null,
                null, null, ContentType.JSON, DeliveryType.SERIAL, [], SubscriptionMode.ANYCAST, [], null, null, false, false
        )
    }

    private def createAvroTopic(String topicName) {
        TopicBuilder.topic(topicName)
                .migratedFromJsonType()
                .withContentType(ContentType.AVRO)
                .build()
    }

    private def createTopicInKafka(String kafkaTopicName, int partitionsNumber) {
        adminClient.createTopics([new NewTopic(kafkaTopicName, partitionsNumber, 1 as short)]).all().get()

        def describeTopicsResult = adminClient.describeTopics([kafkaTopicName]).all().get().values()

        assert describeTopicsResult[0].name() == kafkaTopicName
        assert describeTopicsResult[0].partitions()
                .collect { it.partition() }
                .containsAll(0..(partitionsNumber - 1))
    }
}
