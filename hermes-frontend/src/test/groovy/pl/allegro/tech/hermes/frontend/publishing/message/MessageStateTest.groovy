package pl.allegro.tech.hermes.frontend.publishing.message

import spock.lang.Specification

class MessageStateTest extends Specification {

    MessageState state;

    def setup() {
        state = new MessageState();
    }

    def "should set 'fully message read' state and not set delayed 'reading timeout' state"() {
        expect:
        state.setFullyRead()
        !state.setReadingTimeout()
    }

    def "should not set 'fully message read' state on 'reading timeout' state"() {
        expect:
        state.setReadingTimeout()
        !state.setFullyRead()
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
        state.delayed
    }

    def "should not set 'delayed processing' state"() {
        expect:
        state.setSendingToKafkaProducerQueue()
        state.setSendingToKafka()
        !state.setDelayedProcessing()
        !state.delayed
    }

    def "should set 'delayed sending' state from 'sending to kafka' state"() {
        expect:
        state.setSendingToKafkaProducerQueue()
        state.setSendingToKafka()
        state.setDelayedSending()
        state.delayed
    }

    def "should not set 'delayed sending' state from 'sent to kafka'"() {
        expect:
        state.setSendingToKafkaProducerQueue()
        state.setSendingToKafka()
        state.setSentToKafka()
        !state.setDelayedSending()
        !state.delayed
    }
}
