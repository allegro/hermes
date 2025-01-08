package pl.allegro.tech.hermes.management.domain.detection

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import pl.allegro.tech.hermes.api.Topic
import pl.allegro.tech.hermes.management.config.detection.InactiveTopicsDetectionProperties
import pl.allegro.tech.hermes.management.domain.topic.TopicService
import pl.allegro.tech.hermes.test.helper.builder.TopicBuilder
import spock.lang.Specification

import java.time.Clock
import java.time.Duration
import java.time.Instant

import static java.time.temporal.ChronoUnit.DAYS
import static pl.allegro.tech.hermes.api.TopicName.fromQualifiedName

class InactiveTopicsDetectionJobTest extends Specification {

    def topicServiceMock = Mock(TopicService)
    def inactiveTopicsStorageServiceMock = Mock(InactiveTopicsStorageService)
    def metricsRepositoryMock = Mock(LastPublishedMessageMetricsRepository)
    def clockMock = Mock(Clock)
    def inactiveTopicsNotifier = new InMemoryInactiveTopicsNotifier()

    def inactiveTopicsDetectionProperties = new InactiveTopicsDetectionProperties(
            Duration.ofDays(7),
            Duration.ofDays(14),
            ["group.topic3"] as Set<String>,
            5
    )

    InactiveTopicsDetectionService detectionService = new InactiveTopicsDetectionService(
            metricsRepositoryMock,
            inactiveTopicsDetectionProperties,
            clockMock
    )
    static def meterRegistry = new SimpleMeterRegistry()

    InactiveTopicsDetectionJob detectionJob = new InactiveTopicsDetectionJob(
            topicServiceMock,
            inactiveTopicsStorageServiceMock,
            detectionService,
            Optional.of(inactiveTopicsNotifier),
            inactiveTopicsDetectionProperties,
            clockMock,
            meterRegistry
    )

    def "should detect inactive topics and notify when needed"() {
        given:
        def now = Instant.ofEpochMilli(1630600266987L)
        def ago7days = now.minus(7, DAYS)
        def ago14days = now.minus(14, DAYS)
        def ago21days = now.minus(21, DAYS)
        clockMock.instant() >> now

        and: "names of all topics"
        topicServiceMock.getAllTopics() >> [
                topic("group.topic0"),
                topic("group.topic1"),
                topic("group.topic2"),
                topic("group.topic3"),
                topic("group.topic4"),
        ]

        and: "historically saved inactive topics"
        inactiveTopicsStorageServiceMock.getInactiveTopics() >> [
                new InactiveTopic("group.topic2", ago7days.toEpochMilli(), [], false),
                new InactiveTopic("group.topic3", ago7days.toEpochMilli(), [], true),
                new InactiveTopic("group.topic4", ago21days.toEpochMilli(), [ago14days.toEpochMilli()], false),
        ]

        and: "current last published message timestamp"
        mockLastPublishedMessageTimestamp("group.topic0", now)
        mockLastPublishedMessageTimestamp("group.topic1", ago7days)
        mockLastPublishedMessageTimestamp("group.topic2", now)
        mockLastPublishedMessageTimestamp("group.topic3", ago7days)
        mockLastPublishedMessageTimestamp("group.topic4", ago21days)

        when:
        detectionJob.detectAndNotify()

        then: "notified are inactive topics that are not whitelisted"
        inactiveTopicsNotifier.getNotifiedTopics().toList() == [
                new InactiveTopic("group.topic1", ago7days.toEpochMilli(), [], false),
                new InactiveTopic("group.topic4", ago21days.toEpochMilli(), [ago14days.toEpochMilli()], false)
        ]

        and: "saved are all inactive topics with updated notification timestamps"
        1 * inactiveTopicsStorageServiceMock.markAsInactive([
                new InactiveTopic("group.topic1", ago7days.toEpochMilli(), [now.toEpochMilli()], false),
                new InactiveTopic("group.topic4", ago21days.toEpochMilli(), [now.toEpochMilli(), ago14days.toEpochMilli()], false),
                new InactiveTopic("group.topic3", ago7days.toEpochMilli(), [], true)
        ])

        and:
        inactiveTopicsGauge(0) == 1
        inactiveTopicsGauge(1) == 1
        inactiveTopicsGauge(2) == 1
    }

    def "should not notify if there are no inactive topics"() {
        given:
        topicServiceMock.getAllTopics() >> [topic("group.topic0")]
        inactiveTopicsStorageServiceMock.getInactiveTopics() >> []

        and:
        def now = Instant.ofEpochMilli(1630600266987L)
        clockMock.instant() >> now
        mockLastPublishedMessageTimestamp("group.topic0", now)

        when:
        detectionJob.detectAndNotify()

        then:
        inactiveTopicsNotifier.getNotifiedTopics().toList() == []

        and:
        1 * inactiveTopicsStorageServiceMock.markAsInactive([])
    }

    def "should not save new notification timestamp when notifier is not provided"() {
        given:
        def job = new InactiveTopicsDetectionJob(
                topicServiceMock,
                inactiveTopicsStorageServiceMock,
                detectionService,
                Optional.empty(),
                inactiveTopicsDetectionProperties,
                clockMock,
                meterRegistry
        )

        and:
        topicServiceMock.getAllTopics() >> [topic("group.topic0")]
        inactiveTopicsStorageServiceMock.getInactiveTopics() >> []

        and:
        def now = Instant.ofEpochMilli(1630600266987L)
        def ago7days = now.minus(7, DAYS)
        clockMock.instant() >> now
        mockLastPublishedMessageTimestamp("group.topic0", ago7days)

        when:
        job.detectAndNotify()

        then: "inactive topic is saved but notification timestamp is not added"
        1 * inactiveTopicsStorageServiceMock.markAsInactive([
                new InactiveTopic("group.topic0", ago7days.toEpochMilli(), [], false)
        ])

        and:
        inactiveTopicsGauge(0) == 1
    }

    def "should not save new notification timestamp when notification did not succeed"() {
        given:
        def notifierMock = Mock(InactiveTopicsNotifier)

        and:
        def job = new InactiveTopicsDetectionJob(
                topicServiceMock,
                inactiveTopicsStorageServiceMock,
                detectionService,
                Optional.of(notifierMock),
                inactiveTopicsDetectionProperties,
                clockMock,
                meterRegistry
        )

        and:
        topicServiceMock.getAllTopics() >> [topic("group.topic0"), topic("group.topic1")]
        inactiveTopicsStorageServiceMock.getInactiveTopics() >> []
        notifierMock.notify(_) >> new NotificationResult(["group.topic0": true, "group.topic1": false])

        and:
        def now = Instant.ofEpochMilli(1630600266987L)
        def ago7days = now.minus(7, DAYS)
        clockMock.instant() >> now
        mockLastPublishedMessageTimestamp("group.topic0", ago7days)
        mockLastPublishedMessageTimestamp("group.topic1", ago7days)

        when:
        job.detectAndNotify()

        then:
        1 * inactiveTopicsStorageServiceMock.markAsInactive([
                new InactiveTopic("group.topic0", ago7days.toEpochMilli(), [now.toEpochMilli()], false),
                new InactiveTopic("group.topic1", ago7days.toEpochMilli(), [], false),
        ])

        and:
        inactiveTopicsGauge(0) == 1
        inactiveTopicsGauge(1) == 1
    }

    private def mockLastPublishedMessageTimestamp(String topicName, Instant instant) {
        metricsRepositoryMock.getLastPublishedMessageTimestamp(fromQualifiedName(topicName)) >> Optional.ofNullable(instant)
    }

    private static Topic topic(String name) {
        return TopicBuilder.topic(name).build();
    }

    private static def inactiveTopicsGauge(int notifications) {
        return meterRegistry.find("inactive-topics").tags("notifications", notifications.toString()).gauge().value()
    }
}
