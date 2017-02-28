package pl.allegro.tech.hermes.frontend.publishing.message

import spock.lang.Specification

class MessageStateTest extends Specification {

    MessageState state;

    def setup() {
        state = new MessageState();
    }

    def "should set 'reading state' and not set 'premature timeout' state"() {
        expect:
        state.setReading()
        !state.setPrematureTimeout()
    }

    def "should set 'fully message read' state and not set delayed 'reading timeout' state"() {
        expect:
        state.setReading()
        state.setFullyRead()
        !state.setReadingTimeout()
    }

    def "should not set 'fully message read' state on 'reading timeout' state"() {
        expect:
        state.setReading()
        state.setReadingTimeout()
        !state.setFullyRead()
    }

    def "should set 'reading timeout' state and not set 'reading error' state"() {
        expect:
        state.setReading()
        state.setReadingTimeout()
        !state.setReadingError()
    }

    def "should set 'reading error' state and not set 'reading timeout' state"() {
        expect:
        state.setReading()
        state.setReadingError()
        !state.setReadingTimeout()
    }

    def "should set 'sent to kafka' state from 'sending to kafka' state"() {
        expect:
        state.setSendingToKafkaProducerQueue()
        state.setSendingToKafka()
        state.setSentToKafka()
    }

    def "should set 'sent to kafka' state from 'sending to kafka producer queue' state"() {
        expect:
        state.setSendingToKafkaProducerQueue()
        state.setSentToKafka()
    }

    def "should not set 'sending to kafka' state from 'error in sending to kafka producer queue' state"() {
        expect:
        state.setSendingToKafkaProducerQueue()
        state.setErrorInSendingToKafka()
        !state.setSendingToKafka()
    }

    def "should set 'delayed processing' state"() {
        expect:
        state.setTimeoutHasPassed()
        state.setSendingToKafkaProducerQueue()
        state.setSendingToKafka()
        state.setDelayedProcessing()
    }

    def "should not set 'delayed processing' state"() {
        expect:
        state.setSendingToKafkaProducerQueue()
        state.setSendingToKafka()
        !state.setDelayedProcessing()
    }

    def "should set 'delayed sending' state from 'sending to kafka' state"() {
        expect:
        state.setSendingToKafkaProducerQueue()
        state.setSendingToKafka()
        state.setDelayedSending()
    }

    def "should not set 'delayed sending' state from 'sent to kafka'"() {
        expect:
        state.setSendingToKafkaProducerQueue()
        state.setSendingToKafka()
        state.setSentToKafka()
        !state.setDelayedSending()
    }

    def "should set 'delayed sent' state from 'delayed processing'"() {
        expect:
        state.setSendingToKafkaProducerQueue()
        state.setSendingToKafka()
        state.setTimeoutHasPassed()
        state.setDelayedProcessing()
        state.setDelayedSentToKafka()
    }

    def "should set 'delayed sent' state from 'delayed sending'"() {
        expect:
        state.setSendingToKafkaProducerQueue()
        state.setSendingToKafka()
        state.setDelayedSending()
        state.setDelayedSentToKafka()
    }

    def "should not set 'delayed sent' state from 'sent to kafka'"() {
        expect:
        state.setSendingToKafkaProducerQueue()
        state.setSendingToKafka()
        state.setSentToKafka()
        !state.setDelayedSentToKafka()
    }
}
