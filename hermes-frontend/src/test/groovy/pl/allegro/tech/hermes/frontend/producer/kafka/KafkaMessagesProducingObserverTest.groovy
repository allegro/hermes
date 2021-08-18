package pl.allegro.tech.hermes.frontend.producer.kafka

import pl.allegro.tech.hermes.frontend.producer.BrokerMessagesProducingException
import pl.allegro.tech.hermes.frontend.producer.BrokerMessagesBatchProducingResults
import spock.lang.Specification
import spock.lang.Unroll

import static pl.allegro.tech.hermes.frontend.producer.BrokerMessagesProducingResult.FAILURE
import static pl.allegro.tech.hermes.frontend.producer.BrokerMessagesProducingResult.SUCCESS

@Unroll
class KafkaMessagesProducingObserverTest extends Specification {

    def "should observe results of messages producing to broker if #testScenario"() {
        given:
        KafkaMessagesProducingObserver observer = new KafkaMessagesProducingObserver(brokerMessagesProducingResults.size(), 1000L)

        when:
        brokerMessagesProducingResults.forEach {
            observer.notifyAboutBrokerMessageProducingResult(it)
        }

        and:
        BrokerMessagesBatchProducingResults results = observer.waitForMessagesBatchProducingResults()

        then:
        results.isFailure() == isFailed

        where:
        testScenario                            | brokerMessagesProducingResults || isFailed
        "all messages producing succeeded"      | [SUCCESS, SUCCESS]             || false
        "all messages producing failed"         | [FAILURE, FAILURE]             || true
        "at least one message producing failed" | [SUCCESS, FAILURE]             || true
    }

    def "should throw exception about timeout if #testScenario"() {
        given:
        KafkaMessagesProducingObserver observer = new KafkaMessagesProducingObserver(brokerMessagesProducingResults.size(), 1000L)

        when:
        (0..<timeToNotify).forEach {
            observer.notifyAboutBrokerMessageProducingResult(brokerMessagesProducingResults[it])
        }

        and:
        observer.waitForMessagesBatchProducingResults()

        then:
        BrokerMessagesProducingException exception = thrown(BrokerMessagesProducingException)
        exception.message == "Timeout while publishing messages"

        where:
        testScenario                                                      | brokerMessagesProducingResults | timeToNotify
        "was not notified about at least on result of messages producing" | [SUCCESS, SUCCESS]             | 1
        "was not notified about all results of messages producing"        | [FAILURE, FAILURE]             | 0
    }

    def "should throw exception about invalid argument if #testScenario"() {
        when:
        new KafkaMessagesProducingObserver(messagesCounter, timeoutMs)

        then:
        IllegalArgumentException exception = thrown(IllegalArgumentException)
        exception.message == expectedErrorMessage

        where:
        testScenario                                | messagesCounter | timeoutMs || expectedErrorMessage
        "number messages to observe is invalid"     | -100            | 1000L     || "Must observe positive number of messages to be produced"
        "timeout for messages observing is invalid" | 10              | 0         || "Timeout must be defined"
    }
}
