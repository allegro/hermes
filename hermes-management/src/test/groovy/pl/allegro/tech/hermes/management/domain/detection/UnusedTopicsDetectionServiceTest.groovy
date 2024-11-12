package pl.allegro.tech.hermes.management.domain.detection

import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.management.config.detection.UnusedTopicsDetectionProperties
import spock.lang.Specification

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

class UnusedTopicsDetectionServiceTest extends Specification {

    private static def TEST_TOPIC_NAME = "group.topic"
    private static def WHITELISTED_TOPIC_NAME = "whitelisted.topic"
    private static def LAST_PUBLISHED = Instant.ofEpochMilli(1630600266987L)
    private static def INACTIVITY_THRESHOLD = 7
    private static def NEXT_NOTIFICATION_THRESHOLD = 14

    UnusedTopicsDetectionProperties properties = new UnusedTopicsDetectionProperties(
            Duration.ofDays(INACTIVITY_THRESHOLD),
            Duration.ofDays(NEXT_NOTIFICATION_THRESHOLD),
            [WHITELISTED_TOPIC_NAME] as Set<String>,
            "dc"
    )

    private def metricsRepositoryMock = Mock(LastPublishedMessageMetricsRepository)
    private def clockMock = Mock(Clock)
    private def service = new UnusedTopicsDetectionService(metricsRepositoryMock, properties, clockMock)

    def setup() {
        metricsRepositoryMock.getLastPublishedMessageTimestamp(TopicName.fromQualifiedName(TEST_TOPIC_NAME)) >> LAST_PUBLISHED
        metricsRepositoryMock.getLastPublishedMessageTimestamp(TopicName.fromQualifiedName(WHITELISTED_TOPIC_NAME)) >> LAST_PUBLISHED
    }

    def "should detect unused topic when it surpasses inactivity threshold"() {
        given:
        clockMock.instant() >> now

        when:
        def result = service.detectUnusedTopic(
                TopicName.fromQualifiedName(TEST_TOPIC_NAME),
                historicalUnusedTopic
        )

        then:
        result.get() == new UnusedTopic(
                TEST_TOPIC_NAME,
                LAST_PUBLISHED.toEpochMilli(),
                historicalUnusedTopic.map { it.notificationTimestampsMs() }.orElse([]),
                false
        )

        where:
        now                                                | historicalUnusedTopic
        plusDays(LAST_PUBLISHED, INACTIVITY_THRESHOLD)     | Optional.of(unusedTopic(TEST_TOPIC_NAME, LAST_PUBLISHED, [now]))
        plusDays(LAST_PUBLISHED, INACTIVITY_THRESHOLD)     | Optional.empty()
        plusDays(LAST_PUBLISHED, INACTIVITY_THRESHOLD + 3) | Optional.of(unusedTopic(TEST_TOPIC_NAME, LAST_PUBLISHED, [now]))
    }

    def "should correctly detect unused topic as whitelisted or not"() {
        given:
        def now = plusDays(LAST_PUBLISHED, INACTIVITY_THRESHOLD)
        def historicalUnusedTopic = unusedTopic(topicName, LAST_PUBLISHED, [now], historicallyWhitelisted)

        and:
        clockMock.instant() >> now

        when:
        def result = service.detectUnusedTopic(
                TopicName.fromQualifiedName(topicName),
                Optional.of(historicalUnusedTopic)
        )

        then:
        result.get() == new UnusedTopic(
                topicName,
                LAST_PUBLISHED.toEpochMilli(),
                historicalUnusedTopic.notificationTimestampsMs(),
                whitelisted
        )

        where:
        topicName              | historicallyWhitelisted || whitelisted
        TEST_TOPIC_NAME        | false                   || false
        TEST_TOPIC_NAME        | true                    || false
        WHITELISTED_TOPIC_NAME | false                   || true
        WHITELISTED_TOPIC_NAME | true                    || true
    }

    def "should not detect unused topic when it is within inactivity threshold"() {
        given:
        def now = plusDays(LAST_PUBLISHED, INACTIVITY_THRESHOLD - 1)

        and:
        clockMock.instant() >> now

        when:
        def result = service.detectUnusedTopic(
                TopicName.fromQualifiedName(TEST_TOPIC_NAME),
                historicalUnusedTopic
        )

        then:
        result.isEmpty()

        where:
        historicalUnusedTopic << [
                Optional.of(unusedTopic(TEST_TOPIC_NAME, LAST_PUBLISHED, [])),
                Optional.empty(),
        ]
    }

    def "should be notified when inactive and no sent notifications"() {
        given:
        clockMock.instant() >> now

        and:
        def unusedTopic = unusedTopic(TEST_TOPIC_NAME, LAST_PUBLISHED, [])

        expect:
        service.shouldBeNotified(unusedTopic)

        where:
        now << [plusDays(LAST_PUBLISHED, INACTIVITY_THRESHOLD), plusDays(LAST_PUBLISHED, 2 * INACTIVITY_THRESHOLD)]
    }

    def "should be notified when inactive and enough time from last notification"() {
        given:
        clockMock.instant() >> now

        and:
        def unusedTopic = unusedTopic(TEST_TOPIC_NAME, LAST_PUBLISHED, notificationTimestamps)

        expect:
        service.shouldBeNotified(unusedTopic)

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
        def unusedTopic = unusedTopic(TEST_TOPIC_NAME, LAST_PUBLISHED, [])

        expect:
        !service.shouldBeNotified(unusedTopic)
    }

    def "should not be notified when whitelisted"() {
        given:
        def now = plusDays(LAST_PUBLISHED, INACTIVITY_THRESHOLD)

        and:
        clockMock.instant() >> now

        and:
        def unusedTopic = unusedTopic(TEST_TOPIC_NAME, LAST_PUBLISHED, [], true)

        expect:
        !service.shouldBeNotified(unusedTopic)
    }

    def "should not be notified when not enough time from last notification"() {
        given:
        clockMock.instant() >> now

        and:
        def unusedTopic = unusedTopic(TEST_TOPIC_NAME, LAST_PUBLISHED, notificationTimestamps)

        expect:
        !service.shouldBeNotified(unusedTopic)

        where:
        now                                                                              | notificationTimestamps
        plusDays(LAST_PUBLISHED, INACTIVITY_THRESHOLD)                                   | [now]
        plusDays(LAST_PUBLISHED, INACTIVITY_THRESHOLD + NEXT_NOTIFICATION_THRESHOLD + 1) | [plusDays(LAST_PUBLISHED, INACTIVITY_THRESHOLD + NEXT_NOTIFICATION_THRESHOLD), plusDays(LAST_PUBLISHED, INACTIVITY_THRESHOLD)]
    }

    private static Instant plusDays(Instant instant, int days) {
        return instant.plus(days, ChronoUnit.DAYS)
    }

    private static UnusedTopic unusedTopic(String name, Instant lastPublished, List<Instant> notificationTimestamps, boolean whitelisted = false) {
        return new UnusedTopic(
                name,
                lastPublished.toEpochMilli(),
                notificationTimestamps.collect { it.toEpochMilli() },
                whitelisted
        )
    }
}
