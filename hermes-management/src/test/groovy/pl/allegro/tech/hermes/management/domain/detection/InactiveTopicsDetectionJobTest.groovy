package pl.allegro.tech.hermes.management.domain.detection


import pl.allegro.tech.hermes.management.config.detection.InactiveTopicsDetectionProperties
import pl.allegro.tech.hermes.management.domain.topic.TopicService
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
            "dc"
    )

    InactiveTopicsDetectionService detectionService = new InactiveTopicsDetectionService(
            metricsRepositoryMock,
            inactiveTopicsDetectionProperties,
            clockMock
    )

    InactiveTopicsDetectionJob detectionJob = new InactiveTopicsDetectionJob(
            topicServiceMock,
            inactiveTopicsStorageServiceMock,
            detectionService,
            Optional.of(inactiveTopicsNotifier),
            clockMock
    )

    def "should detect inactive topics and notify when needed"() {
        given:
        def now = Instant.ofEpochMilli(1630600266987L)
        def ago7days = now.minus(7, DAYS)
        def ago14days = now.minus(14, DAYS)
        def ago21days = now.minus(21, DAYS)
        clockMock.instant() >> now

        and: "names of all topics"
        topicServiceMock.listQualifiedTopicNames() >> [
                "group.topic0",
                "group.topic1",
                "group.topic2",
                "group.topic3",
                "group.topic4",
        ]

        and: "historically saved inactive topics"
        inactiveTopicsStorageServiceMock.getInactiveTopics() >> [
                new InactiveTopic("group.topic2", ago7days.toEpochMilli(), [], false),
                new InactiveTopic("group.topic3", ago7days.toEpochMilli(), [], true),
                new InactiveTopic("group.topic4", ago21days.toEpochMilli(), [ago14days.toEpochMilli()], false),
        ]

        and: "current last published message timestamp"
        metricsRepositoryMock.getLastPublishedMessageTimestamp(fromQualifiedName("group.topic0")) >> now
        metricsRepositoryMock.getLastPublishedMessageTimestamp(fromQualifiedName("group.topic1")) >> ago7days
        metricsRepositoryMock.getLastPublishedMessageTimestamp(fromQualifiedName("group.topic2")) >> now
        metricsRepositoryMock.getLastPublishedMessageTimestamp(fromQualifiedName("group.topic3")) >> ago7days
        metricsRepositoryMock.getLastPublishedMessageTimestamp(fromQualifiedName("group.topic4")) >> ago21days

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
                new InactiveTopic("group.topic4", ago21days.toEpochMilli(), [ago14days.toEpochMilli(), now.toEpochMilli()], false),
                new InactiveTopic("group.topic3", ago7days.toEpochMilli(), [], true)
        ])
    }

    def "should not notify if there are no inactive topics"() {
        given:
        topicServiceMock.listQualifiedTopicNames() >> ["group.topic0"]
        inactiveTopicsStorageServiceMock.getInactiveTopics() >> []

        and:
        def now = Instant.ofEpochMilli(1630600266987L)
        clockMock.instant() >> now
        metricsRepositoryMock.getLastPublishedMessageTimestamp(fromQualifiedName("group.topic0")) >> now

        when:
        detectionJob.detectAndNotify()

        then:
        inactiveTopicsNotifier.getNotifiedTopics().toList() == []

        and:
        1 * inactiveTopicsStorageServiceMock.markAsInactive([])
    }
}
