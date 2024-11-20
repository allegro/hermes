package pl.allegro.tech.hermes.api.constraints


import jakarta.validation.ConstraintValidatorContext
import pl.allegro.tech.hermes.api.OfflineRetransmissionFromTopicRequest
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Instant

class RetransmissionTimeRangeValidatorTest extends Specification {

    ConstraintValidatorContext mockContext = Mock()

    @Shared
    def lowerTimestamp = Instant.now().toString()
    @Shared
    def higherTimestamp = Instant.now().plusSeconds(5).toString()

    @Unroll
    def "Time range validator should validate topic retransmission request when startTimestamp is '#startTimestamp', endTimestamp is '#endTimestamp'"() {
        given:
        TimeRangeForTopicRetransmissionValidator validator = new TimeRangeForTopicRetransmissionValidator()
        def request = new OfflineRetransmissionFromTopicRequest(
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
}
