package pl.allegro.tech.hermes.management.domain.detection


import pl.allegro.tech.hermes.management.config.detection.UnusedTopicsDetectionProperties
import pl.allegro.tech.hermes.management.domain.topic.TopicService
import spock.lang.Specification

import java.time.Clock
import java.time.Duration
import java.time.Instant

import static java.time.temporal.ChronoUnit.DAYS
import static pl.allegro.tech.hermes.api.TopicName.fromQualifiedName

class UnusedTopicsDetectionJobTest extends Specification {

    def topicServiceMock = Mock(TopicService)
    def unusedTopicsStorageServiceMock = Mock(UnusedTopicsStorageService)
    def metricsRepositoryMock = Mock(LastPublishedMessageMetricsRepository)
    def clockMock = Mock(Clock)
    def unusedTopicsNotifier = new InMemoryUnusedTopicsNotifier()

    def unusedTopicsDetectionProperties = new UnusedTopicsDetectionProperties(
            Duration.ofDays(7),
            Duration.ofDays(14),
            ["group.topic3"] as Set<String>,
            "dc"
    )

    UnusedTopicsDetectionService detectionService = new UnusedTopicsDetectionService(
            metricsRepositoryMock,
            unusedTopicsDetectionProperties,
            clockMock
    )

    UnusedTopicsDetectionJob detectionJob = new UnusedTopicsDetectionJob(
            topicServiceMock,
            unusedTopicsStorageServiceMock,
            detectionService,
            Optional.of(unusedTopicsNotifier),
            clockMock
    )

    def "should detect unused topics and notify when needed"() {
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

        and: "historically saved unused topics"
        unusedTopicsStorageServiceMock.getUnusedTopics() >> [
                new UnusedTopic("group.topic2", ago7days.toEpochMilli(), [], false),
                new UnusedTopic("group.topic3", ago7days.toEpochMilli(), [], true),
                new UnusedTopic("group.topic4", ago21days.toEpochMilli(), [ago14days.toEpochMilli()], false),
        ]

        and: "current last published message timestamp"
        metricsRepositoryMock.getLastPublishedMessageTimestamp(fromQualifiedName("group.topic0")) >> now
        metricsRepositoryMock.getLastPublishedMessageTimestamp(fromQualifiedName("group.topic1")) >> ago7days
        metricsRepositoryMock.getLastPublishedMessageTimestamp(fromQualifiedName("group.topic2")) >> now
        metricsRepositoryMock.getLastPublishedMessageTimestamp(fromQualifiedName("group.topic3")) >> ago7days
        metricsRepositoryMock.getLastPublishedMessageTimestamp(fromQualifiedName("group.topic4")) >> ago21days

        when:
        detectionJob.detectAndNotify()

        then: "notified are unused topics that are not whitelisted"
        unusedTopicsNotifier.getNotifiedTopics().toList() == [
                new UnusedTopic("group.topic1", ago7days.toEpochMilli(), [], false),
                new UnusedTopic("group.topic4", ago21days.toEpochMilli(), [ago14days.toEpochMilli()], false)
        ]

        and: "saved are all unused topics with updated notification timestamps"
        1 * unusedTopicsStorageServiceMock.markAsUnused([
                new UnusedTopic("group.topic1", ago7days.toEpochMilli(), [now.toEpochMilli()], false),
                new UnusedTopic("group.topic4", ago21days.toEpochMilli(), [ago14days.toEpochMilli(), now.toEpochMilli()], false),
                new UnusedTopic("group.topic3", ago7days.toEpochMilli(), [], true)
        ])
    }

    def "should not notify if there are no unused topics"() {
        given:
        topicServiceMock.listQualifiedTopicNames() >> ["group.topic0"]
        unusedTopicsStorageServiceMock.getUnusedTopics() >> []

        and:
        def now = Instant.ofEpochMilli(1630600266987L)
        clockMock.instant() >> now
        metricsRepositoryMock.getLastPublishedMessageTimestamp(fromQualifiedName("group.topic0")) >> now

        when:
        detectionJob.detectAndNotify()

        then:
        unusedTopicsNotifier.getNotifiedTopics().toList() == []

        and:
        1 * unusedTopicsStorageServiceMock.markAsUnused([])
    }
}
