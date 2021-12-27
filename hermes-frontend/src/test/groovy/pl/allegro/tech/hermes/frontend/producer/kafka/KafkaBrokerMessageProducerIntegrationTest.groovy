package pl.allegro.tech.hermes.frontend.producer.kafka

import com.codahale.metrics.MetricRegistry
import com.jayway.awaitility.Awaitility
import org.apache.commons.lang3.tuple.ImmutablePair
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.TopicPartition
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.spock.Testcontainers
import pl.allegro.tech.hermes.api.ContentType
import pl.allegro.tech.hermes.api.DeliveryType
import pl.allegro.tech.hermes.api.Subscription
import pl.allegro.tech.hermes.api.SubscriptionMode
import pl.allegro.tech.hermes.api.Topic
import pl.allegro.tech.hermes.common.config.ConfigFactory
import pl.allegro.tech.hermes.common.config.Configs
import pl.allegro.tech.hermes.common.kafka.ConsumerGroupId
import pl.allegro.tech.hermes.common.kafka.JsonToAvroMigrationKafkaNamesMapper
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper
import pl.allegro.tech.hermes.common.metric.HermesMetrics
import pl.allegro.tech.hermes.frontend.metric.CachedTopic
import pl.allegro.tech.hermes.frontend.publishing.avro.AvroMessage
import pl.allegro.tech.hermes.frontend.server.CachedTopicsTestHelper
import pl.allegro.tech.hermes.metrics.PathsCompiler
import pl.allegro.tech.hermes.test.helper.avro.AvroUser
import pl.allegro.tech.hermes.test.helper.builder.TopicBuilder
import spock.lang.Shared
import spock.lang.Specification

import java.time.Duration
import java.util.stream.Collectors

import static java.util.concurrent.TimeUnit.MILLISECONDS
import static org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG

@Testcontainers
class KafkaBrokerMessageProducerIntegrationTest extends Specification {

    static Integer NUMBER_OF_PARTITION = 3

    @Shared
    KafkaContainer kafkaContainer = new KafkaContainer()

    @Shared
    ConfigFactory configFactory = Mock() {
        getStringProperty(Configs.KAFKA_HEADER_NAME_MESSAGE_ID) >> "id"
        getStringProperty(Configs.KAFKA_HEADER_NAME_TIMESTAMP) >> "ts"
        getStringProperty(Configs.KAFKA_HEADER_NAME_SCHEMA_VERSION) >> "sv"
        getStringProperty(Configs.KAFKA_HEADER_NAME_SCHEMA_ID) >> "sid"
    }

    @Shared
    KafkaProducer<byte[], byte[]> leaderConfirms

    @Shared
    KafkaProducer<byte[], byte[]> everyoneConfirms

    @Shared
    KafkaBrokerMessageProducer brokerMessageProducer

    @Shared
    KafkaNamesMapper kafkaNamesMapper = new JsonToAvroMigrationKafkaNamesMapper("")

    @Shared
    Producers producers

    @Shared
    String containerId

    @Shared
    AdminClient adminClient


    def setupSpec() {
        kafkaContainer.start()
        kafkaContainer.waitingFor(Wait.forHealthcheck())
        containerId = kafkaContainer.containerId

        Properties props = new Properties()
        props.put(BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.bootstrapServers)
        props.put(KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer")
        props.put(VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArraySerializer")
        leaderConfirms = new KafkaProducer<byte[], byte[]>(props)
        everyoneConfirms = new KafkaProducer<byte[], byte[]>(props)
        adminClient = AdminClient.create(props)
    }

    def setup() {
        producers = new Producers(leaderConfirms, everyoneConfirms, configFactory)
        brokerMessageProducer = new KafkaBrokerMessageProducer(producers,
                new KafkaTopicMetadataFetcher(adminClient, configFactory),
                new HermesMetrics(new MetricRegistry(), new PathsCompiler("localhost")),
                new KafkaHeaderFactory(configFactory),
                configFactory)
    }

    def "should publish messages on only one partition for the same partition-key"() {
        Topic topic = createAvroTopic("pl.allegro.test.Foo")
        Subscription subscription = createTestSubscription(topic, "test-subscription")
        String kafkaTopicName = topic.getName().toString()
        ConsumerGroupId consumerGroupId = kafkaNamesMapper.toConsumerGroupId(subscription.qualifiedName)
        createTopicInKafka(kafkaTopicName, NUMBER_OF_PARTITION)
        CachedTopic cachedTopic = CachedTopicsTestHelper.cachedTopic(topic)
        KafkaConsumer consumer = createConsumer(consumerGroupId, kafkaTopicName)

        when:
        1.upto(10) {
            brokerMessageProducer.send(generateAvroMessage("partition-key"), cachedTopic, null)
            waitForRecordPublishing(consumer)
        }

        then:
        consumer.close()

        List<OffsetAndMetadata> partitionsWithMessagesData = adminClient
                .listConsumerGroupOffsets(consumerGroupId.asString())
                .partitionsToOffsetAndMetadata()
                .get().values().stream()
                .filter { metadata -> metadata.offset() != 0 }
                .collect(Collectors.toList())

        partitionsWithMessagesData.size() == 1
        partitionsWithMessagesData.get(0).offset() == 10
    }

    def "should publish messages with random distribiution when pratition-key is not present"() {
        Topic topic = createAvroTopic("pl.allegro.test.randomFoo")
        Subscription subscription = createTestSubscription(topic, "test-subscription")
        String kafkaTopicName = topic.getName().toString()
        ConsumerGroupId consumerGroupId = kafkaNamesMapper.toConsumerGroupId(subscription.qualifiedName)
        createTopicInKafka(kafkaTopicName, NUMBER_OF_PARTITION)
        CachedTopic cachedTopic = CachedTopicsTestHelper.cachedTopic(topic)
        KafkaConsumer consumer = createConsumer(consumerGroupId, kafkaTopicName)

        when:
        1.upto(10) {
            brokerMessageProducer.send(generateAvroMessage(null), cachedTopic, null)
            waitForRecordPublishing(consumer)
        }

        then:
        consumer.close()

        List<OffsetAndMetadata> partitionsWithMessagesData = adminClient
                .listConsumerGroupOffsets(consumerGroupId.asString())
                .partitionsToOffsetAndMetadata()
                .get().values().stream()
                .filter { metadata -> metadata.offset() != 0 }
                .collect(Collectors.toList())

        partitionsWithMessagesData.size() == NUMBER_OF_PARTITION
    }

    private AvroMessage generateAvroMessage(String partitionKey) {
        def avroUser = new AvroUser()
        return new AvroMessage(UUID.randomUUID().toString(), avroUser.asBytes(), 0L, avroUser.compiledSchema, partitionKey)
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

    private static def waitForRecordPublishing(KafkaConsumer consumer) {
        Awaitility.await()
                .pollInterval(200, MILLISECONDS)
                .atMost(1000, MILLISECONDS)
                .until {
                    consumer.poll(Duration.ofMillis(150)).count() == 1
                }
    }

    private def createTopicInKafka(String kafkaTopicName, int partitionsNumber) {
        adminClient.createTopics([new NewTopic(kafkaTopicName, partitionsNumber, 1 as short)]).all().get()

        def describeTopicsResult = adminClient.describeTopics([kafkaTopicName]).all().get().values()

        assert describeTopicsResult[0].name() == kafkaTopicName
        assert describeTopicsResult[0].partitions()
                .collect { it.partition() }
                .containsAll(0..(partitionsNumber - 1))
    }

    private KafkaConsumer<String, String> createConsumer(ConsumerGroupId groupId, String kafkaTopicName) {
        Properties props = new Properties()
        props.put(BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.bootstrapServers)
        props.put(GROUP_ID_CONFIG, groupId.asString())
        props.put(KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArrayDeserializer")
        props.put(VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.ByteArrayDeserializer")

        KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<>(props);

        Set<TopicPartition> topicPartitions = kafkaConsumer.partitionsFor(kafkaTopicName)
                .collect { new TopicPartition(it.topic(), it.partition()) }
        kafkaConsumer.assign(topicPartitions);
        Map<TopicPartition, OffsetAndMetadata> topicPartitionByOffset = topicPartitions.stream()
                .collect({
                    long offset = kafkaConsumer.position(it);
                    return ImmutablePair.of(it, new OffsetAndMetadata(offset));
                }).collectEntries()
        kafkaConsumer.commitSync(topicPartitionByOffset);

        return kafkaConsumer
    }
}
