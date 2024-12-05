package pl.allegro.tech.hermes.management.domain.detection

import spock.lang.Specification

import java.time.Instant

class InactiveTopicTest extends Specification {
    def "should update notification timestamps"() {
        given:
        def inactiveTopic = new InactiveTopic(
                "group.topic",
                1732616853320L,
                [1732616853325L],
                false
        )

        when:
        def result = inactiveTopic.notificationSent(Instant.ofEpochMilli(1732616853330L))

        then:
        result == new InactiveTopic(
                "group.topic",
                1732616853320L,
                [1732616853325L, 1732616853330L],
                false
        )
    }

    def "should limit notification timestamps size and leave only latest"() {
        given:
        def inactiveTopic = new InactiveTopic(
                "group.topic",
                1732616853320L,
                [1732616853330L, 1732616853325L, 1732616853335L],
                false
        )

        when:
        def result = inactiveTopic.limitNotificationsHistory(2)

        then:
        result == new InactiveTopic(
                "group.topic",
                1732616853320L,
                [1732616853335L, 1732616853330L],
                false
        )
    }
}
