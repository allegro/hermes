package pl.allegro.tech.hermes.frontend.publishing.message

import spock.lang.Specification

class MessageStateTest extends Specification {

    def "should set 'fully message read' state and not set delayed 'reading timeout' state"() {
        given:
        MessageState state = new MessageState()
        boolean fullyRead = false
        boolean timeout = false

        when:
        state.onFullyReadSet({ fullyRead = true })
        state.onReadingTimeoutSet({ timeout = true })

        then:
        fullyRead
        !timeout
    }

    def "should not set 'fully message read' state on 'reading timeout' state"() {
        given:
        MessageState state = new MessageState()
        boolean fullyRead = false
        boolean timeout = false

        when:
        state.onReadingTimeoutSet({ timeout = true })
        state.onFullyReadSet({ fullyRead = true })

        then:
        timeout
        !fullyRead
    }

    def "should set 'sent to kafka' state from 'sending to kafka' state"() {
        given:
        MessageState state = new MessageState()
        boolean sendingToKafka = false
        boolean sentToKafka = false

        when:
        state.setSendingToKafkaProducerQueue()
        state.onSendingToKafkaSet({ sendingToKafka = true })
        state.onSentToKafkaSet({ sentToKafka = true })

        then:
        sendingToKafka
        sentToKafka
    }

    def "should set 'sent to kafka' state from 'sending to kafka producer queue' state"() {
        given:
        MessageState state = new MessageState()
        boolean sentToKafka = false

        when:
        state.setSendingToKafkaProducerQueue()
        state.onSentToKafkaSet({ sentToKafka = true })

        then:
        sentToKafka
    }

    def "should not set 'sending to kafka' state from 'error in sending to kafka producer queue' state"() {
        given:
        MessageState state = new MessageState()
        boolean sendingToKafka = false

        when:
        state.setSendingToKafkaProducerQueue()
        state.setErrorInSendingToKafka()
        state.onSendingToKafkaSet({ sendingToKafka = true })

        then:
        !sendingToKafka
    }

    def "should set 'delayed processing' state"() {
        given:
        MessageState state = new MessageState()
        boolean delayedProcessing = false
        boolean delayed = false

        when:
        state.setDelayedProcessing()
        state.setSendingToKafkaProducerQueue()
        state.onSendingToKafkaSet({})
        state.onDelayedProcessingSet({ delayedProcessing = true })
        state.onDelayed({ delayed = true })

        then:
        delayedProcessing
        delayed
    }

    def "should not set 'delayed processing' state"() {
        given:
        MessageState state = new MessageState()
        boolean delayedProcessing = false
        boolean delayed = false

        when:
        state.setSendingToKafkaProducerQueue()
        state.onSendingToKafkaSet({})
        state.onDelayedProcessingSet({ delayedProcessing = true })
        state.onDelayed({ delayed = true })

        then:
        !delayedProcessing
        !delayed
    }

    def "should set 'delayed sending' state from 'sending to kafka' state"() {
        given:
        MessageState state = new MessageState()
        boolean delayedSending = false
        boolean delayed = false

        when:
        state.setSendingToKafkaProducerQueue()
        state.onSendingToKafkaSet({})
        state.onDelayedSendingSet({ delayedSending = true })
        state.onDelayed({ delayed = true })

        then:
        delayedSending
        delayed
    }

    def "should not set 'delayed sending' state from 'sent to kafka'"() {
        given:
        MessageState state = new MessageState()
        boolean delayedSending = false
        boolean delayed = false

        when:
        state.setSendingToKafkaProducerQueue()
        state.onSendingToKafkaSet({})
        state.onSentToKafkaSet({ })
        state.onDelayedSendingSet({ delayedSending = true })
        state.onDelayed({ delayed = true })

        then:
        !delayedSending
        !delayed
    }
}
