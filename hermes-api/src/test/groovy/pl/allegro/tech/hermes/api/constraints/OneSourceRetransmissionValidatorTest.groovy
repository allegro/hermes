package pl.allegro.tech.hermes.api.constraints


import jakarta.validation.ConstraintValidatorContext
import pl.allegro.tech.hermes.api.OfflineRetransmissionRequest
import spock.lang.Specification

class OneSourceRetransmissionValidatorTest extends Specification {

    OneSourceRetransmissionValidator validator = new OneSourceRetransmissionValidator()
    ConstraintValidatorContext mockContext = Mock()

    def "Validator should validate retransmission request when sourceView is '#sourceView' and sourceTopic is '#sourceTopic'"() {
        given:
        def request = new OfflineRetransmissionRequest(
                sourceView,
                sourceTopic,
                "someTargetTopic",
                "2024-07-08T12:00:00",
                "2024-07-08T13:00:00"
        )
        expect:
        validator.isValid(request, mockContext) == isValid

        where:
        sourceView | sourceTopic | isValid
        null       | "testTopic" | true
        "testView" | null        | true
        null       | null        | false
        "testView" | "testTopic" | false
        ""         | ""          | false
        "  "       | "  "        | false
        ""         | "testTopic" | false
        "testView" | "  "        | false
    }

}
