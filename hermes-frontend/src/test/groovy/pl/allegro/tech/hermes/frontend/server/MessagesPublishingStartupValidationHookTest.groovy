package pl.allegro.tech.hermes.frontend.server

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import org.glassfish.hk2.api.ServiceLocator
import pl.allegro.tech.hermes.common.config.ConfigFactory
import pl.allegro.tech.hermes.common.config.Configs
import pl.allegro.tech.hermes.common.message.wrapper.MessageContentWrapper
import pl.allegro.tech.hermes.frontend.cache.topic.TopicsCache
import pl.allegro.tech.hermes.frontend.metric.CachedTopic
import pl.allegro.tech.hermes.frontend.producer.BrokerMessagesProducer
import pl.allegro.tech.hermes.frontend.producer.BrokerMessagesProducingException
import pl.allegro.tech.hermes.frontend.producer.BrokerMessagesBatchProducingResults
import pl.allegro.tech.hermes.frontend.publishing.message.JsonMessage
import spock.lang.Shared
import spock.lang.Specification

import static pl.allegro.tech.hermes.frontend.producer.BrokerMessagesProducingResult.FAILURE
import static pl.allegro.tech.hermes.frontend.producer.BrokerMessagesProducingResult.SUCCESS
import static pl.allegro.tech.hermes.frontend.server.CachedTopicsTestHelper.cachedTopic

class MessagesPublishingStartupValidationHookTest extends Specification {

    @Shared
    ServiceLocator serviceLocator = Mock()
    @Shared
    String topicValidationName = "group.test"
    @Shared
    long validationTimeout = 100L
    @Shared
    int validationMessagesCount = 3
    @Shared
    CachedTopic validationTopic = cachedTopic(topicValidationName)
    @Shared
    int validationRetryCount = 2
    @Shared
    Clock clock = Clock.fixed(Instant.ofEpochMilli(12345), ZoneId.systemDefault())
    @Shared
    MessageContentWrapper messageContentWrapper = Mock()

    @Shared
    ConfigFactory config = Mock() {
        getIntProperty(Configs.BROKER_PUBLISHING_STARTUP_VALIDATION_MESSAGES_COUNT) >> validationMessagesCount
        getLongProperty(Configs.BROKER_PUBLISHING_STARTUP_VALIDATION_TIMEOUT_MS) >> validationTimeout
        getStringProperty(Configs.BROKER_PUBLISHING_STARTUP_VALIDATION_TOPIC_NAME) >> topicValidationName
        getLongProperty(Configs.BROKER_PUBLISHING_STARTUP_VALIDATION_RETRY_INTERVAL) >> 1L
    }

    @Shared
    TopicsCache topicsCache = Mock() {
        getTopic(topicValidationName) >> Optional.of(validationTopic)
    }

    def "should not throw exception if all messages were successfully published"() {
        given:
        BrokerMessagesProducer producer = Spy()
        MessagesPublishingStartupValidationHook hook = new MessagesPublishingStartupValidationHook(config, producer, messageContentWrapper, clock, topicsCache)

        when:
        hook.accept(serviceLocator)

        then:
        1 * producer.publishMessages(
                validationTopic, {
            it instanceof List<JsonMessage>
            (it as List<JsonMessage>).size() == validationMessagesCount
        }, validationTimeout) >> successPublishingStartupValidationResults()
    }

    def "should retry if publishing one message failed"() {
        given:
        BrokerMessagesProducer producer = Spy()
        MessagesPublishingStartupValidationHook hook = new MessagesPublishingStartupValidationHook(config, producer, messageContentWrapper, clock, topicsCache)

        when:
        hook.accept(serviceLocator)

        then:
        validationRetryCount * producer.publishMessages(
                validationTopic, {
            it instanceof List<JsonMessage>
            (it as List<JsonMessage>).size() == validationMessagesCount
        }, validationTimeout) >> failedPublishingStartupValidationResults()
        1 * producer.publishMessages(
                validationTopic, {
            it instanceof List<JsonMessage>
            (it as List<JsonMessage>).size() == validationMessagesCount
        }, validationTimeout) >> successPublishingStartupValidationResults()
    }

    def "should retry if exception was thrown while publishing to broker"() {
        given:
        BrokerMessagesProducer producer = Mock()
        MessagesPublishingStartupValidationHook hook = new MessagesPublishingStartupValidationHook(config, producer, messageContentWrapper, clock, topicsCache)

        when:
        hook.accept(serviceLocator)

        then:
        validationRetryCount * producer.publishMessages(
                validationTopic, {
            it instanceof List<JsonMessage>
            (it as List<JsonMessage>).size() == validationMessagesCount
        }, validationTimeout) >> { throw new BrokerMessagesProducingException("error message") }
        1 * producer.publishMessages(
                validationTopic, {
            it instanceof List<JsonMessage>
            (it as List<JsonMessage>).size() == validationMessagesCount
        }, validationTimeout) >> successPublishingStartupValidationResults()
    }

    def "should throw exception if there is not cached topic for publishing validation"() {
        given:
        BrokerMessagesProducer producer = Spy()
        MessagesPublishingStartupValidationHook hook = new MessagesPublishingStartupValidationHook(config, producer, messageContentWrapper, clock, Mock(TopicsCache.class) { getTopic(topicValidationName) >> Optional.empty() })

        when:
        hook.accept(serviceLocator)

        then:
        IllegalStateException exception = thrown(IllegalStateException)
        exception.message == "Missing topic to validate publishing messages on startup"
    }

    def "should throw exception if failed validation results occurred and max duration for publishing exceeded"() {
        given:
        BrokerMessagesProducer producer = Mock() {
            publishMessages(validationTopic, _ as List<JsonMessage>, validationTimeout) >> failedPublishingStartupValidationResults()
        }
        MessagesPublishingStartupValidationHook hook = new MessagesPublishingStartupValidationHook(config, producer, messageContentWrapper, clock, topicsCache)

        when:
        hook.accept(serviceLocator)

        then:
        PublishingStartupValidationException exception = thrown(PublishingStartupValidationException)
        exception.message == "Error while validating publishing messages, last result: failed:1, success:2"
    }

    def "should throw exception if exception was thrown while publishing to broker and max duration for publishing exceeded"() {
        given:
        BrokerMessagesProducer producer = Mock()
        producer.publishMessages(validationTopic, _ as List<JsonMessage>, validationTimeout) >> { throw new BrokerMessagesProducingException("error message") }
        MessagesPublishingStartupValidationHook hook = new MessagesPublishingStartupValidationHook(config, producer, messageContentWrapper, clock, topicsCache)

        when:
        hook.accept(serviceLocator)

        then:
        PublishingStartupValidationException exception = thrown(PublishingStartupValidationException)
        exception.message == "Error while validating publishing messages, last result: null"
    }

    private static BrokerMessagesBatchProducingResults successPublishingStartupValidationResults() {
        new BrokerMessagesBatchProducingResults([SUCCESS, SUCCESS, SUCCESS])
    }

    private static BrokerMessagesBatchProducingResults failedPublishingStartupValidationResults() {
        new BrokerMessagesBatchProducingResults([FAILURE, SUCCESS, SUCCESS])
    }
}
