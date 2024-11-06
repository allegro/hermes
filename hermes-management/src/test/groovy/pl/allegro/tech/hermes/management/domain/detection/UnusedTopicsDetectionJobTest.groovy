package pl.allegro.tech.hermes.management.domain.detection


import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.management.config.detection.UnusedTopicsDetectionProperties
import pl.allegro.tech.hermes.management.domain.topic.TopicService
import spock.lang.Specification

import java.time.Clock
import java.time.Duration
import java.time.Instant

import static java.time.temporal.ChronoUnit.DAYS

class UnusedTopicsDetectionJobTest extends Specification {

    def topicServiceMock = Mock(TopicService)
    def unusedTopicsServiceMock = Mock(UnusedTopicsService)
    def metricsRepositoryMock = Mock(LastPublishedMessageMetricsRepository)
    def clockMock = Mock(Clock)
    def unusedTopicsNotifier = new InMemoryUnusedTopicsNotifier()

    def unusedTopicsDetectionProperties = new UnusedTopicsDetectionProperties(
            Duration.ofDays(7),
            Duration.ofDays(14),
            ["group.topic3"] as Set<String>
    )

    UnusedTopicsDetectionService detectionService = new UnusedTopicsDetectionService(
            metricsRepositoryMock,
            unusedTopicsDetectionProperties,
            clockMock
    )

    UnusedTopicsDetectionJob detectionJob = new UnusedTopicsDetectionJob(
            topicServiceMock,
            unusedTopicsServiceMock,
            detectionService,
            unusedTopicsNotifier,
            clockMock
    )

    def "should detect unused topics and notify when needed"() {
        given:
        def now = Instant.ofEpochMilli(1630600266987L)
        def ago7days = now.minus(7, DAYS)
        def ago14days = now.minus(14, DAYS)
        def ago21days = now.minus(21, DAYS)
        clockMock.instant() >> now

        and:
        topicServiceMock.listQualifiedTopicNames() >> [
                "group.topic0",
                "group.topic1",
                "group.topic2",
                "group.topic3",
                "group.topic4",
        ]

        and: "historically saved unused topics"
        unusedTopicsServiceMock.getUnusedTopics() >> [
                new UnusedTopic("group.topic2", ago7days.toEpochMilli(), [], false),
                new UnusedTopic("group.topic3", ago7days.toEpochMilli(), [], true),
                new UnusedTopic("group.topic4", ago21days.toEpochMilli(), [ago14days.toEpochMilli()], false),
        ]

        and: "current last published message timestamp"
        metricsRepositoryMock.getLastPublishedMessageTimestamp(TopicName.fromQualifiedName("group.topic0")) >> now
        metricsRepositoryMock.getLastPublishedMessageTimestamp(TopicName.fromQualifiedName("group.topic1")) >> ago7days
        metricsRepositoryMock.getLastPublishedMessageTimestamp(TopicName.fromQualifiedName("group.topic2")) >> now
        metricsRepositoryMock.getLastPublishedMessageTimestamp(TopicName.fromQualifiedName("group.topic3")) >> ago7days
        metricsRepositoryMock.getLastPublishedMessageTimestamp(TopicName.fromQualifiedName("group.topic4")) >> ago21days

        when:
        detectionJob.detectAndNotify()

        then:
        unusedTopicsNotifier.getNotifiedTopics().toList() == [
                new UnusedTopic("group.topic1", ago7days.toEpochMilli(), [], false),
                new UnusedTopic("group.topic4", ago21days.toEpochMilli(), [ago14days.toEpochMilli()], false)
        ]

        1 * unusedTopicsServiceMock.markAsUnused([
                new UnusedTopic("group.topic1", ago7days.toEpochMilli(), [now.toEpochMilli()], false),
                new UnusedTopic("group.topic4", ago21days.toEpochMilli(), [ago14days.toEpochMilli(), now.toEpochMilli()], false),
                new UnusedTopic("group.topic3", ago7days.toEpochMilli(), [], true)
        ])
    }
}
