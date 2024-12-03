package pl.allegro.tech.hermes.management.domain.detection

import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.management.config.detection.InactiveTopicsDetectionProperties
import spock.lang.Specification

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

class InactiveTopicsDetectionServiceTest extends Specification {

    private static def TEST_TOPIC_NAME = "group.topic"
    private static def WHITELISTED_TOPIC_NAME = "whitelisted.topic"
    private static def LAST_PUBLISHED = Instant.ofEpochMilli(1630600266987L)
    private static def INACTIVITY_THRESHOLD = 7
    private static def NEXT_NOTIFICATION_THRESHOLD = 14

    InactiveTopicsDetectionProperties properties = new InactiveTopicsDetectionProperties(
            Duration.ofDays(INACTIVITY_THRESHOLD),
            Duration.ofDays(NEXT_NOTIFICATION_THRESHOLD),
            [WHITELISTED_TOPIC_NAME] as Set<String>,
            5
    )

    private def metricsRepositoryMock = Mock(LastPublishedMessageMetricsRepository)
    private def clockMock = Mock(Clock)
    private def service = new InactiveTopicsDetectionService(metricsRepositoryMock, properties, clockMock)

    def setup() {
        metricsRepositoryMock.getLastPublishedMessageTimestamp(TopicName.fromQualifiedName(TEST_TOPIC_NAME)) >> Optional.of(LAST_PUBLISHED)
        metricsRepositoryMock.getLastPublishedMessageTimestamp(TopicName.fromQualifiedName(WHITELISTED_TOPIC_NAME)) >> Optional.of(LAST_PUBLISHED)
    }

    def "should detect inactive topic when it surpasses inactivity threshold"() {
        given:
        clockMock.instant() >> now

        when:
        def result = service.detectInactiveTopic(
                TopicName.fromQualifiedName(TEST_TOPIC_NAME),
                historicalInactiveTopic
        )

        then:
        result.get() == new InactiveTopic(
                TEST_TOPIC_NAME,
                LAST_PUBLISHED.toEpochMilli(),
                historicalInactiveTopic.map { it.notificationTimestampsMs() }.orElse([]),
                false
        )

        where:
        now                                                | historicalInactiveTopic
        plusDays(LAST_PUBLISHED, INACTIVITY_THRESHOLD)     | Optional.of(inactiveTopic(TEST_TOPIC_NAME, LAST_PUBLISHED, [now]))
        plusDays(LAST_PUBLISHED, INACTIVITY_THRESHOLD)     | Optional.empty()
        plusDays(LAST_PUBLISHED, INACTIVITY_THRESHOLD + 3) | Optional.of(inactiveTopic(TEST_TOPIC_NAME, LAST_PUBLISHED, [now]))
    }

    def "should correctly detect inactive topic as whitelisted or not"() {
        given:
        def now = plusDays(LAST_PUBLISHED, INACTIVITY_THRESHOLD)
        def historicalInactiveTopic = inactiveTopic(topicName, LAST_PUBLISHED, [now], historicallyWhitelisted)

        and:
        clockMock.instant() >> now

        when:
        def result = service.detectInactiveTopic(
                TopicName.fromQualifiedName(topicName),
                Optional.of(historicalInactiveTopic)
        )

        then:
        result.get() == new InactiveTopic(
                topicName,
                LAST_PUBLISHED.toEpochMilli(),
                historicalInactiveTopic.notificationTimestampsMs(),
                whitelisted
        )

        where:
        topicName              | historicallyWhitelisted || whitelisted
        TEST_TOPIC_NAME        | false                   || false
        TEST_TOPIC_NAME        | true                    || false
        WHITELISTED_TOPIC_NAME | false                   || true
        WHITELISTED_TOPIC_NAME | true                    || true
    }

    def "should not detect inactive topic when it is within inactivity threshold"() {
        given:
        def now = plusDays(LAST_PUBLISHED, INACTIVITY_THRESHOLD - 1)

        and:
        clockMock.instant() >> now

        when:
        def result = service.detectInactiveTopic(
                TopicName.fromQualifiedName(TEST_TOPIC_NAME),
                historicalInactiveTopic
        )

        then:
        result.isEmpty()

        where:
        historicalInactiveTopic << [
                Optional.of(inactiveTopic(TEST_TOPIC_NAME, LAST_PUBLISHED, [])),
                Optional.empty(),
        ]
    }

    def "should not detect inactive topic when last published message metrics are not available"() {
        given:
        String topicName = "group.topicWithoutMetrics"

        and:
        clockMock.instant() >> plusDays(LAST_PUBLISHED, INACTIVITY_THRESHOLD)
        metricsRepositoryMock.getLastPublishedMessageTimestamp(TopicName.fromQualifiedName(topicName)) >> Optional.empty()

        when:
        def result = service.detectInactiveTopic(
                TopicName.fromQualifiedName(topicName),
                Optional.empty()
        )

        then:
        result.isEmpty()
    }

    def "should be notified when inactive and no sent notifications"() {
        given:
        clockMock.instant() >> now

        and:
        def inactiveTopic = inactiveTopic(TEST_TOPIC_NAME, LAST_PUBLISHED, [])

        expect:
        service.shouldBeNotified(inactiveTopic)

        where:
        now << [plusDays(LAST_PUBLISHED, INACTIVITY_THRESHOLD), plusDays(LAST_PUBLISHED, 2 * INACTIVITY_THRESHOLD)]
    }

    def "should be notified when inactive and enough time from last notification"() {
        given:
        clockMock.instant() >> now

        and:
        def inactiveTopic = inactiveTopic(TEST_TOPIC_NAME, LAST_PUBLISHED, notificationTimestamps)

        expect:
        service.shouldBeNotified(inactiveTopic)

        where:
        now                                                                              | notificationTimestamps
        plusDays(LAST_PUBLISHED, INACTIVITY_THRESHOLD + NEXT_NOTIFICATION_THRESHOLD)     | [plusDays(LAST_PUBLISHED, INACTIVITY_THRESHOLD)]
        plusDays(LAST_PUBLISHED, INACTIVITY_THRESHOLD + 2 * NEXT_NOTIFICATION_THRESHOLD) | [plusDays(LAST_PUBLISHED, INACTIVITY_THRESHOLD + NEXT_NOTIFICATION_THRESHOLD), plusDays(LAST_PUBLISHED, INACTIVITY_THRESHOLD)]
    }

    def "should not be notified if not inactive"() {
        given:
        def now = plusDays(LAST_PUBLISHED, INACTIVITY_THRESHOLD - 1)

        and:
        clockMock.instant() >> now

        and:
        def inactiveTopic = inactiveTopic(TEST_TOPIC_NAME, LAST_PUBLISHED, [])

        expect:
        !service.shouldBeNotified(inactiveTopic)
    }

    def "should not be notified when whitelisted"() {
        given:
        def now = plusDays(LAST_PUBLISHED, INACTIVITY_THRESHOLD)

        and:
        clockMock.instant() >> now

        and:
        def inactiveTopic = inactiveTopic(TEST_TOPIC_NAME, LAST_PUBLISHED, [], true)

        expect:
        !service.shouldBeNotified(inactiveTopic)
    }

    def "should not be notified when not enough time from last notification"() {
        given:
        clockMock.instant() >> now

        and:
        def inactiveTopic = inactiveTopic(TEST_TOPIC_NAME, LAST_PUBLISHED, notificationTimestamps)

        expect:
        !service.shouldBeNotified(inactiveTopic)

        where:
        now                                                                              | notificationTimestamps
        plusDays(LAST_PUBLISHED, INACTIVITY_THRESHOLD)                                   | [now]
        plusDays(LAST_PUBLISHED, INACTIVITY_THRESHOLD + NEXT_NOTIFICATION_THRESHOLD + 1) | [plusDays(LAST_PUBLISHED, INACTIVITY_THRESHOLD + NEXT_NOTIFICATION_THRESHOLD), plusDays(LAST_PUBLISHED, INACTIVITY_THRESHOLD)]
    }

    private static Instant plusDays(Instant instant, int days) {
        return instant.plus(days, ChronoUnit.DAYS)
    }

    private static InactiveTopic inactiveTopic(String name, Instant lastPublished, List<Instant> notificationTimestamps, boolean whitelisted = false) {
        return new InactiveTopic(
                name,
                lastPublished.toEpochMilli(),
                notificationTimestamps.collect { it.toEpochMilli() },
                whitelisted
        )
    }
}
