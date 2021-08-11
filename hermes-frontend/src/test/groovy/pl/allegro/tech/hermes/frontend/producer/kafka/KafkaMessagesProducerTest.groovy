package pl.allegro.tech.hermes.frontend.producer.kafka

import pl.allegro.tech.hermes.frontend.metric.CachedTopic
import pl.allegro.tech.hermes.frontend.producer.BrokerMessageProducer
import pl.allegro.tech.hermes.frontend.producer.BrokerMessagesProducingResults
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback
import pl.allegro.tech.hermes.frontend.publishing.message.JsonMessage
import pl.allegro.tech.hermes.frontend.publishing.message.Message
import spock.lang.Shared
import spock.lang.Specification

import static com.google.common.base.Charsets.UTF_8
import static pl.allegro.tech.hermes.frontend.server.CachedTopicsTestHelper.cachedTopic

class KafkaMessagesProducerTest extends Specification {

    @Shared
    CachedTopic cachedTopic = cachedTopic("group.test")

    def "should successfully publish messages if publishing single message on broker is successful"() {
        given:
        List<Message> jsonMessages = (0..<4).collect {jsonMessage(it)}
        BrokerMessageProducer brokerMessageProducer = TestBrokerMessageProducer.builder().throwException(false).build()
        KafkaMessagesProducer kafkaMessagesProducer = new KafkaMessagesProducer(brokerMessageProducer)

         when:
        BrokerMessagesProducingResults results = kafkaMessagesProducer.publishMessages(cachedTopic, jsonMessages, 100L)

        then:
        !results.isFailure()
    }

    def "should fail to publish messages if exception is thrown by publishing single message on broker"() {
        given:
        List<Message> jsonMessages = (0..<4).collect {jsonMessage(it)}
        BrokerMessageProducer brokerMessageProducer = TestBrokerMessageProducer.builder().throwException(true).build()
        KafkaMessagesProducer kafkaMessagesProducer = new KafkaMessagesProducer(brokerMessageProducer)

        when:
        kafkaMessagesProducer.publishMessages(cachedTopic, jsonMessages, 100L)

        then:
        thrown(RuntimeException)
    }

    def "should fail to publish messages if any single message publishing on broker failed"() {
        given:
        List<Message> successfulJsonMessages = (0..<3).collect {jsonMessage(it)}
        Message failedJsonMessage = jsonMessage(3)
        BrokerMessageProducer brokerMessageProducer = TestBrokerMessageProducer.builder().failedPublishedMessages([failedJsonMessage]).build()
        KafkaMessagesProducer kafkaMessagesProducer = new KafkaMessagesProducer(brokerMessageProducer)

        when:
        BrokerMessagesProducingResults results = kafkaMessagesProducer.publishMessages(cachedTopic, successfulJsonMessages + failedJsonMessage, 100L)

        then:
        results.isFailure()
        results.toString() == "failed:1, success:3"
    }

    def "should not try to publish messages if there is no messages"() {
        given:
        BrokerMessageProducer brokerMessageProducer = Mock()
        KafkaMessagesProducer kafkaMessagesProducer = new KafkaMessagesProducer(brokerMessageProducer)

        when:
        BrokerMessagesProducingResults results = kafkaMessagesProducer.publishMessages(cachedTopic, [], 100L)

        then:
        !results.isFailure()
        0 * brokerMessageProducer.send(_ as JsonMessage, _ as CachedTopic, _ as PublishingCallback)
    }

    private static JsonMessage jsonMessage(int id) {
        new JsonMessage(id.toString(), "{\"data\":\"json\"}".getBytes(UTF_8), 1L, null)
    }
}
