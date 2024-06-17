package pl.allegro.tech.hermes.infrastructure.zookeeper

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.databind.ObjectMapper
import org.awaitility.Awaitility
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
        setupNode('/hermes/consumers-workload-constraints/group.topic', new Constraints(1))
        setupNode('/hermes/consumers-workload-constraints/group.topic$sub', new Constraints(3))
        ensureCacheWasUpdated(2)

        when:
        def constraints = pathChildrenCache.getConsumersWorkloadConstraints()

        then:
        constraints.topicConstraints == [(TopicName.fromQualifiedName('group.topic')): new Constraints(1)]
        constraints.subscriptionConstraints == [(SubscriptionName.fromString('group.topic$sub')): new Constraints(3)]
    }

    def "should update cache on create node"() {
        when:
        setupNode('/hermes/consumers-workload-constraints/group.topic', new Constraints(1))
        ensureCacheWasUpdated(1)
        def constraints = pathChildrenCache.getConsumersWorkloadConstraints()

        then:
        constraints.topicConstraints.get(TopicName.fromQualifiedName('group.topic')).consumersNumber == 1
        constraints.subscriptionConstraints == emptyMap()

        when:
        setupNode('/hermes/consumers-workload-constraints/group.topic$sub', new Constraints(3))
        ensureCacheWasUpdated(2)
        def updatedConstraints = pathChildrenCache.getConsumersWorkloadConstraints()

        then:
        updatedConstraints.topicConstraints.get(TopicName.fromQualifiedName('group.topic')).consumersNumber == 1
        updatedConstraints.subscriptionConstraints.get(SubscriptionName.fromString('group.topic$sub')).consumersNumber == 3
    }

    def "should update cache on delete node"() {
        when:
        setupNode('/hermes/consumers-workload-constraints/group.topic', new Constraints(1))
        setupNode('/hermes/consumers-workload-constraints/group.topic$sub', new Constraints(3))
        ensureCacheWasUpdated(2)
        def constraints = pathChildrenCache.getConsumersWorkloadConstraints()

        then:
        constraints.topicConstraints.get(TopicName.fromQualifiedName('group.topic')).consumersNumber == 1
        constraints.subscriptionConstraints.get(SubscriptionName.fromString('group.topic$sub')).consumersNumber == 3

        when:
        deleteData('/hermes/consumers-workload-constraints/group.topic')
        ensureCacheWasUpdated(1)
        def updatedConstraints = pathChildrenCache.getConsumersWorkloadConstraints()

        then:
        updatedConstraints.topicConstraints == emptyMap()
        constraints.subscriptionConstraints.get(SubscriptionName.fromString('group.topic$sub')).consumersNumber == 3
    }

    def "should update cache on change node"() {
        given:
        setupNode('/hermes/consumers-workload-constraints/group.topic', new Constraints(1))
        setupNode('/hermes/consumers-workload-constraints/group.topic$sub', new Constraints(3))
        ensureCacheWasUpdated(2)

        when:
        def constraints = pathChildrenCache.getConsumersWorkloadConstraints()

        then:
        constraints.topicConstraints.get(TopicName.fromQualifiedName('group.topic')).consumersNumber == 1
        constraints.subscriptionConstraints.get(SubscriptionName.fromString('group.topic$sub')).consumersNumber == 3

        when:
        updateNode('/hermes/consumers-workload-constraints/group.topic', new Constraints(2))
        ensureCacheWasUpdated(2)

        def updatedConstraints = pathChildrenCache.getConsumersWorkloadConstraints()

        then:
        updatedConstraints.topicConstraints.get(TopicName.fromQualifiedName('group.topic')).consumersNumber == 2
        updatedConstraints.subscriptionConstraints.get(SubscriptionName.fromString('group.topic$sub')).consumersNumber == 3
    }

    def "should log error if cannot read data from node"() {
        given:
        setupNode('/hermes/consumers-workload-constraints/group.topic', new Constraints(1))
        setupNode('/hermes/consumers-workload-constraints/group.topic$sub', new Constraints(3))
        ensureCacheWasUpdated(2)

        when:
        def constraints = pathChildrenCache.getConsumersWorkloadConstraints()

        then:
        constraints.topicConstraints.get(TopicName.fromQualifiedName('group.topic')).consumersNumber == 1
        constraints.subscriptionConstraints.get(SubscriptionName.fromString('group.topic$sub')).consumersNumber == 3

        when:
        updateNode('/hermes/consumers-workload-constraints/group.topic', 'random data')
        ensureCacheWasUpdated(2)

        def updatedConstraints = pathChildrenCache.getConsumersWorkloadConstraints()

        then: 'data remained intact'
        updatedConstraints.topicConstraints.get(TopicName.fromQualifiedName('group.topic')).consumersNumber == 1
        updatedConstraints.subscriptionConstraints.get(SubscriptionName.fromString('group.topic$sub')).consumersNumber == 3

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

