package pl.allegro.tech.hermes.consumers.consumer.receiver

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import pl.allegro.tech.hermes.common.kafka.KafkaNamesMapper
import pl.allegro.tech.hermes.common.kafka.NamespaceKafkaNamesMapper
import pl.allegro.tech.hermes.common.metric.HermesMetrics
import pl.allegro.tech.hermes.consumers.consumer.Message
import pl.allegro.tech.hermes.consumers.consumer.load.SubscriptionLoadRecorder
import pl.allegro.tech.hermes.consumers.consumer.offset.ConsumerPartitionAssignmentState
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.KafkaConsumerRecordToMessageConverter
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.KafkaConsumerRecordToMessageConverterFactory
import pl.allegro.tech.hermes.consumers.consumer.receiver.kafka.KafkaSingleThreadedMessageReceiver
import pl.allegro.tech.hermes.consumers.test.MessageBuilder
import pl.allegro.tech.hermes.schema.SchemaExistenceEnsurer
import pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder
import pl.allegro.tech.hermes.test.helper.builder.TopicBuilder
import spock.lang.Specification

import java.time.Duration

class KafkaSingleThreadedMessageReceiverTest extends Specification {
    KafkaSingleThreadedMessageReceiver receiver
    KafkaConsumer<byte[], byte[]> consumer = Mock(KafkaConsumer)
    KafkaConsumerRecordToMessageConverterFactory converterFactory = Mock(KafkaConsumerRecordToMessageConverterFactory)
    KafkaConsumerRecordToMessageConverter messageConverter = Mock(KafkaConsumerRecordToMessageConverter)
    KafkaNamesMapper kafkaNamesMapper = new NamespaceKafkaNamesMapper("namespace", "_ ")

    def topic = TopicBuilder.topic("pl.allegro.someTestTopic").build()
    def subscription = SubscriptionBuilder.subscription(topic, "someSub").build()

    def setup() {
        converterFactory.create(*_) >> messageConverter
        receiver = new KafkaSingleThreadedMessageReceiver(
                consumer, converterFactory, Mock(HermesMetrics),
                kafkaNamesMapper, topic, subscription, Duration.ofMillis(10), 10,
                Mock(SubscriptionLoadRecorder),
                Mock(ConsumerPartitionAssignmentState)
        )
    }

    def "should return message after it is successfully converted"() {
        given:
        def consumerRecord = new ConsumerRecord<byte[], byte[]>(
                "pl.allegro.someTestTopic", 0, 0, "data".getBytes(), "data".getBytes())
        def kafkaRecords = new ConsumerRecords<byte[], byte[]>(
                Map.of(new TopicPartition(topic.qualifiedName, 0), [consumerRecord])
        )
        def message = MessageBuilder.testMessage()

        consumer.poll(Duration.ofMillis(10)) >> kafkaRecords
        2 * messageConverter.convertToMessage(*_) >>
                { throw new SchemaExistenceEnsurer.SchemaNotLoaded("msg") } >>
                message

        when:
        Optional<Message> result = receiver.next()

        then:
        result.isEmpty()

        when:
        result = receiver.next()

        then:
        result.isPresent()
        result.get() == message
    }
}