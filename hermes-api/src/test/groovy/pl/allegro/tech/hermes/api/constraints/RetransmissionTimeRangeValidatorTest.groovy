package pl.allegro.tech.hermes.api.constraints


import jakarta.validation.ConstraintValidatorContext
import pl.allegro.tech.hermes.api.OfflineRetransmissionRequest
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Instant

class RetransmissionTimeRangeValidatorTest extends Specification {

    ConstraintValidatorContext mockContext = Mock()

    @Shared
    def someTimestamp = Instant.now().toString()
    @Shared
    def lowerTimestamp = Instant.now().toString()
    @Shared
    def higherTimestamp = Instant.now().plusSeconds(5).toString()

    @Unroll
    def "View validator should validate view retransmission request when startTimestamp is '#startTimestamp', endTimestamp is '#endTimestamp'"() {
        given:
        TimeRangeForViewRetransmissionValidator validator = new TimeRangeForViewRetransmissionValidator()
        def request = new OfflineRetransmissionRequest(
                "someSourceView",
                null,
                "someTargetTopic",
                startTimestamp,
                endTimestamp
        )
        expect:
        validator.isValid(request, mockContext) == isValid

        where:
        startTimestamp | endTimestamp  | isValid
        null           | null          | true
        someTimestamp  | null          | false
        null           | someTimestamp | false
        someTimestamp  | someTimestamp | false
    }

    def "View validator should skip validation for topic retransmission request"() {
        given:
        TimeRangeForViewRetransmissionValidator validator = new TimeRangeForViewRetransmissionValidator()
        def invalidTopicRetransmissionRequest = new OfflineRetransmissionRequest(
                null,
                "someSourceTopic",
                "someTargetTopic",
                null,
                null
        )
        expect:
        validator.isValid(invalidTopicRetransmissionRequest, mockContext)
    }

    @Unroll
    def "Topic validator should validate topic retransmission request when startTimestamp is '#startTimestamp', endTimestamp is '#endTimestamp'"() {
        given:
        TimeRangeForTopicRetransmissionValidator validator = new TimeRangeForTopicRetransmissionValidator()
        def request = new OfflineRetransmissionRequest(
                null,
                "someSourceTopic",
                "someTargetTopic",
                startTimestamp,
                endTimestamp
        )
        expect:
        validator.isValid(request, mockContext) == isValid

        where:
        startTimestamp  | endTimestamp    | isValid
        lowerTimestamp  | higherTimestamp | true
        null            | higherTimestamp | false
        lowerTimestamp  | null            | false
        null            | null            | false
        higherTimestamp | lowerTimestamp  | false
        lowerTimestamp  | lowerTimestamp  | false
    }

    def "Topic validator should skip validation for view retransmission request"() {
        given:
        TimeRangeForTopicRetransmissionValidator validator = new TimeRangeForTopicRetransmissionValidator()
        def invalidViewRetransmissionRequest = new OfflineRetransmissionRequest(
                "someSourceView",
                null,
                "someTargetTopic",
                someTimestamp,
                someTimestamp
        )
        expect:
        validator.isValid(invalidViewRetransmissionRequest, mockContext)
    }

}
