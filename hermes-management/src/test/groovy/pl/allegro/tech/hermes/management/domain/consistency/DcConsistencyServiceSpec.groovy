package pl.allegro.tech.hermes.management.domain.consistency

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import pl.allegro.tech.hermes.api.Group
import pl.allegro.tech.hermes.api.Subscription
import pl.allegro.tech.hermes.api.Topic
import pl.allegro.tech.hermes.common.metric.MetricsFacade
import pl.allegro.tech.hermes.management.config.ConsistencyCheckerProperties
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.time.Duration

import static pl.allegro.tech.hermes.test.helper.builder.GroupBuilder.group
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic

class DcConsistencyServiceSpec extends Specification {

    def objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
    def meterRegistry = new SimpleMeterRegistry()
    def metricsFacade = new MetricsFacade(meterRegistry)

    def "should return empty list when given groups are consistent"() {
        given:
        Group group = group("testGroup").build()
        Topic topic = topic(group.groupName, "testTopic").build()
        Subscription subscription = subscription(topic, "testSubscription").build()
        MockRepositoryManager repositoryManager = new MockRepositoryManager()
        repositoryManager.datacenter("dc1")
                .addGroup(group)
                .addTopic(topic)
                .addSubscription(subscription)
        repositoryManager.datacenter("dc2")
                .addGroup(group)
                .addTopic(topic)
                .addSubscription(subscription)
        DcConsistencyService dcConsistencyService = new DcConsistencyService(repositoryManager,
                objectMapper, new ConsistencyCheckerProperties(), metricsFacade)

        when:
        def inconsistentGroups = dcConsistencyService.listInconsistentGroups([group.groupName] as Set)

        then:
        inconsistentGroups == []
    }

    def "should return groups with inconsistent metadata"() {
        MockRepositoryManager repositoryManager = new MockRepositoryManager()
        repositoryManager.datacenter("dc1")
                .addGroup(group("testGroup").build())
                .addGroup(group("testGroup-dc1").build())
        repositoryManager.datacenter("dc2")
                .addGroup(group("testGroup").build())
                .addGroup(group("testGroup-dc2").build())
        DcConsistencyService consistencyService = new DcConsistencyService(repositoryManager,
                objectMapper, new ConsistencyCheckerProperties(), metricsFacade)

        when:
        def groups = consistencyService.listInconsistentGroups(["testGroup", "testGroup-dc1", "testGroup-dc2"] as Set)

        then:
        groups.collect { it.name } as Set == ["testGroup-dc1", "testGroup-dc2"] as Set
    }

    def "should return groups with inconsistent topics"() {
        Group group = group("testGroup").build()
        MockRepositoryManager repositoryManager = new MockRepositoryManager()
        repositoryManager.datacenter("dc1")
                .addGroup(group)
                .addTopic(topic(group.groupName, "testTopic").withDescription("dc1").build())
        repositoryManager.datacenter("dc2")
                .addGroup(group)
                .addTopic(topic(group.groupName, "testTopic").withDescription("dc2").build())
        DcConsistencyService consistencyService = new DcConsistencyService(repositoryManager, objectMapper,
                new ConsistencyCheckerProperties(), metricsFacade)

        when:
        def groups = consistencyService.listInconsistentGroups(["testGroup"] as Set)

        then:
        groups.collect { it.name } as Set == ["testGroup"] as Set
    }

    def "should return groups with inconsistent subscriptions"() {
        Group group = group("testGroup").build()
        Topic topic = topic(group.groupName, "testTopic").build()
        MockRepositoryManager repositoryManager = new MockRepositoryManager()
        repositoryManager.datacenter("dc1")
                .addGroup(group)
                .addTopic(topic)
                .addSubscription(subscription(topic, "testSubscription").withDescription("dc1").build())
        repositoryManager.datacenter("dc2")
                .addGroup(group)
                .addTopic(topic)
                .addSubscription(subscription(topic, "testSubscription").withDescription("dc2").build())
        DcConsistencyService consistencyService = new DcConsistencyService(repositoryManager, objectMapper,
                new ConsistencyCheckerProperties(), metricsFacade)

        when:
        def groups = consistencyService.listInconsistentGroups(["testGroup"] as Set)

        then:
        groups.collect { it.name } as Set == ["testGroup"] as Set
    }

    def "should list group names from all datacenters"() {
        MockRepositoryManager repositoryManager = new MockRepositoryManager()
        repositoryManager.datacenter("dc1")
                .addGroup(group("testGroup").build())
                .addGroup(group("testGroup-dc1").build())
        repositoryManager.datacenter("dc2")
                .addGroup(group("testGroup").build())
                .addGroup(group("testGroup-dc2").build())
        DcConsistencyService consistencyService = new DcConsistencyService(repositoryManager, objectMapper,
                new ConsistencyCheckerProperties(), metricsFacade)

        when:
        def groups = consistencyService.listAllGroupNames()

        then:
        groups == ["testGroup", "testGroup-dc1", "testGroup-dc2"] as Set
    }

    def "should report storage as not consistent with periodic check"() {
        given: "inconsistent storage state"
        MockRepositoryManager repositoryManager = new MockRepositoryManager()
        repositoryManager.datacenter("dc1")
                .addGroup(group("testGroup").build())
                .addGroup(group("testGroup-dc1").build())
        repositoryManager.datacenter("dc2")
                .addGroup(group("testGroup").build())
                .addGroup(group("testGroup-dc2").build())

        and: "enabled periodic consistency checks"
        def properties = new ConsistencyCheckerProperties()
        properties.setPeriodicCheckEnabled(true)
        properties.setInitialRefreshDelay(Duration.ofMillis(0))

        when: "consistency service is created"
        DcConsistencyService consistencyService = new DcConsistencyService(repositoryManager,
                objectMapper,
                properties,
                metricsFacade)

        then: "storage is reported as not consistent"
        new PollingConditions(timeout: 10).eventually {
            meterRegistry.get("storage.consistency").gauge().value() == 0.0d
        }
    }

    def "should report storage as consistent with periodic check"() {
        given: "consistent storage state"
        MockRepositoryManager repositoryManager = new MockRepositoryManager()
        repositoryManager.datacenter("dc1")
                .addGroup(group("testGroup").build())
        repositoryManager.datacenter("dc2")
                .addGroup(group("testGroup").build())

        and: "enabled periodic consistency checks"
        def properties = new ConsistencyCheckerProperties()
        properties.setPeriodicCheckEnabled(true)
        properties.setInitialRefreshDelay(Duration.ofMillis(0))

        when: "consistency service is created"
        DcConsistencyService consistencyService = new DcConsistencyService(repositoryManager,
                objectMapper,
                properties,
                metricsFacade)

        then: "storage is reported as consistent"
        new PollingConditions(timeout: 10).eventually {
            meterRegistry.get("storage.consistency").gauge().value() == 1.0d
        }
    }
}
