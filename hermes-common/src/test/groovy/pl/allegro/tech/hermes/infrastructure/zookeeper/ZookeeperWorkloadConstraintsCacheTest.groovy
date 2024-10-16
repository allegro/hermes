package pl.allegro.tech.hermes.infrastructure.zookeeper

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import pl.allegro.tech.hermes.api.Constraints
import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.domain.workload.constraints.ConsumersWorkloadConstraints
import pl.allegro.tech.hermes.test.IntegrationTest

import java.util.concurrent.TimeUnit

import static java.util.Collections.emptyMap
import static org.awaitility.Awaitility.await

class ZookeeperWorkloadConstraintsCacheTest extends IntegrationTest {

    ZookeeperWorkloadConstraintsCache pathChildrenCache

    Logger logger
    ListAppender<ILoggingEvent> listAppender

    def setup() {
        logger = (Logger) LoggerFactory.getLogger(ZookeeperWorkloadConstraintsCache.class)
        listAppender = new ListAppender<>()
        listAppender.start()
        logger.addAppender(listAppender)

        try {
            deleteAllNodes('/hermes/consumers-workload-constraints')
        } catch (Exception e) {
            e.printStackTrace()
        }

        pathChildrenCache = new ZookeeperWorkloadConstraintsCache(zookeeper(), new ObjectMapper(), new ZookeeperPaths("/hermes"))
        pathChildrenCache.start()
    }

    def cleanup() {
        pathChildrenCache.close()
    }

    def "should return empty constraints"() {
        expect:
        pathChildrenCache.getConsumersWorkloadConstraints() == new ConsumersWorkloadConstraints(emptyMap(), emptyMap())
    }

    def "should return defined constraints"() {
        given:
        setupNode('/hermes/consumers-workload-constraints/group.topic', new Constraints(1, "Some reason"))
        setupNode('/hermes/consumers-workload-constraints/group.topic$sub', new Constraints(3, "Some other reason"))
        ensureCacheWasUpdated(2)

        when:
        def constraints = pathChildrenCache.getConsumersWorkloadConstraints()

        then:
        constraints.topicConstraints == [(TopicName.fromQualifiedName('group.topic')): new Constraints(1, "Some reason")]
        constraints.subscriptionConstraints == [(SubscriptionName.fromString('group.topic$sub')): new Constraints(3, "Some other reason")]
    }

    def "should update cache on create node"() {
        when:
        setupNode('/hermes/consumers-workload-constraints/group.topic', new Constraints(1, "Some reason"))
        ensureCacheWasUpdated(1)
        def constraints = pathChildrenCache.getConsumersWorkloadConstraints()

        then:
        def topicConstraints = constraints.topicConstraints.get(TopicName.fromQualifiedName('group.topic'))
        topicConstraints.consumersNumber == 1
        topicConstraints.reason == "Some reason"
        constraints.subscriptionConstraints == emptyMap()

        when:
        setupNode('/hermes/consumers-workload-constraints/group.topic$sub', new Constraints(3, "Some other reason"))
        ensureCacheWasUpdated(2)
        def updatedConstraints = pathChildrenCache.getConsumersWorkloadConstraints()

        then:
        def updatedTopicConstraints = updatedConstraints.topicConstraints.get(TopicName.fromQualifiedName('group.topic'))
        updatedTopicConstraints.consumersNumber == 1
        updatedTopicConstraints.reason == "Some reason"
        def updatedSubscriptionConstraints = updatedConstraints.subscriptionConstraints.get(SubscriptionName.fromString('group.topic$sub'))
        updatedSubscriptionConstraints.consumersNumber == 3
        updatedSubscriptionConstraints.reason == "Some other reason"
    }

    def "should update cache on delete node"() {
        when:
        setupNode('/hermes/consumers-workload-constraints/group.topic', new Constraints(1, "Some reason"))
        setupNode('/hermes/consumers-workload-constraints/group.topic$sub', new Constraints(3, "Some other reason"))
        ensureCacheWasUpdated(2)
        def constraints = pathChildrenCache.getConsumersWorkloadConstraints()

        then:
        def topicConstraints = constraints.topicConstraints.get(TopicName.fromQualifiedName('group.topic'))
        topicConstraints.consumersNumber == 1
        topicConstraints.reason == "Some reason"
        def subscriptionConstraints = constraints.subscriptionConstraints.get(SubscriptionName.fromString('group.topic$sub'))
        subscriptionConstraints.consumersNumber == 3
        subscriptionConstraints.reason == "Some other reason"

        when:
        deleteData('/hermes/consumers-workload-constraints/group.topic')
        ensureCacheWasUpdated(1)
        def updatedConstraints = pathChildrenCache.getConsumersWorkloadConstraints()

        then:
        updatedConstraints.topicConstraints == emptyMap()
        def updatedSubscriptionConstraints = constraints.subscriptionConstraints.get(SubscriptionName.fromString('group.topic$sub'))
        updatedSubscriptionConstraints.consumersNumber == 3
        updatedSubscriptionConstraints.reason == "Some other reason"
    }

    def "should update cache on change node"() {
        given:
        setupNode('/hermes/consumers-workload-constraints/group.topic', new Constraints(1, "Some reason 1"))
        setupNode('/hermes/consumers-workload-constraints/group.topic$sub', new Constraints(3, "Some reason 3"))
        ensureCacheWasUpdated(2)

        when:
        def constraints = pathChildrenCache.getConsumersWorkloadConstraints()

        then:
        def topicConstraints = constraints.topicConstraints.get(TopicName.fromQualifiedName('group.topic'))
        topicConstraints.consumersNumber == 1
        topicConstraints.reason == "Some reason 1"
        def subscriptionConstraints = constraints.subscriptionConstraints.get(SubscriptionName.fromString('group.topic$sub'))
        subscriptionConstraints.consumersNumber == 3
        subscriptionConstraints.reason == "Some reason 3"

        when:
        updateNode('/hermes/consumers-workload-constraints/group.topic', new Constraints(2, "Some reason 2"))
        ensureCacheWasUpdated(2)

        def updatedConstraints = pathChildrenCache.getConsumersWorkloadConstraints()

        then:
        def updatedTopicConstraints = updatedConstraints.topicConstraints.get(TopicName.fromQualifiedName('group.topic'))
        updatedTopicConstraints.consumersNumber == 2
        updatedTopicConstraints.reason == "Some reason 2"
        def updatedSubscriptionConstraints = updatedConstraints.subscriptionConstraints.get(SubscriptionName.fromString('group.topic$sub'))
        updatedSubscriptionConstraints.consumersNumber == 3
        updatedSubscriptionConstraints.reason == "Some reason 3"

    }

    def "should log error if cannot read data from node"() {
        given:
        setupNode('/hermes/consumers-workload-constraints/group.topic', new Constraints(1, "Some reason"))
        setupNode('/hermes/consumers-workload-constraints/group.topic$sub', new Constraints(3, "Some other reason"))
        ensureCacheWasUpdated(2)

        when:
        def constraints = pathChildrenCache.getConsumersWorkloadConstraints()

        then:
        def topicConstraints = constraints.topicConstraints.get(TopicName.fromQualifiedName('group.topic'))
        topicConstraints.consumersNumber == 1
        topicConstraints.reason == "Some reason"
        def subscriptionConstraints = constraints.subscriptionConstraints.get(SubscriptionName.fromString('group.topic$sub'))
        subscriptionConstraints.consumersNumber == 3
        subscriptionConstraints.reason == "Some other reason"

        when:
        updateNode('/hermes/consumers-workload-constraints/group.topic', 'random data')
        ensureCacheWasUpdated(2)

        def updatedConstraints = pathChildrenCache.getConsumersWorkloadConstraints()

        then: 'data remained intact'
        def updatedTopicConstraints = updatedConstraints.topicConstraints.get(TopicName.fromQualifiedName('group.topic'))
        updatedTopicConstraints.consumersNumber == 1
        updatedTopicConstraints.reason == "Some reason"
        def updatedSubscriptionConstraints = updatedConstraints.subscriptionConstraints.get(SubscriptionName.fromString('group.topic$sub'))
        updatedSubscriptionConstraints.consumersNumber == 3
        updatedSubscriptionConstraints.reason == "Some other reason"

        and:
        listAppender.list.get(0).formattedMessage == 'Cannot read data from node: /hermes/consumers-workload-constraints/group.topic'
        listAppender.list.get(0).throwableProxy.className == 'com.fasterxml.jackson.databind.exc.MismatchedInputException'
        listAppender.list.get(0).level == Level.ERROR
    }

    private def ensureCacheWasUpdated(int expectedSize) {
        await()
                .atMost(200, TimeUnit.MILLISECONDS)
                .until { pathChildrenCache.getCurrentData().size() == expectedSize }
    }
}

