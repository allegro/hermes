package pl.allegro.tech.hermes.management.infrastructure.monitoring

import pl.allegro.tech.hermes.api.Subscription
import pl.allegro.tech.hermes.api.Topic
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.management.config.MonitoringProperties
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionService
import pl.allegro.tech.hermes.management.domain.topic.TopicService
import pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder
import pl.allegro.tech.hermes.test.helper.builder.TopicBuilder
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.concurrent.PollingConditions

import java.time.Duration

import static pl.allegro.tech.hermes.api.Subscription.State.ACTIVE
import static pl.allegro.tech.hermes.api.Subscription.State.PENDING
import static pl.allegro.tech.hermes.api.Subscription.State.SUSPENDED

class MonitoringCacheTest extends Specification {

    private MonitoringProperties monitoringProperties = Mock()
    private SubscriptionService subscriptionService = Mock()
    private TopicService topicService = Mock()
    private MonitoringService monitoringService = Mock()
    private MonitoringServicesCreator monitoringServicesCreator = Mock()

    private TopicName topicName
    private Topic topic
    private Subscription activeSubscription

    void setup() {
        topicName = new TopicName("group", "topic")
        topic = TopicBuilder.topic(topicName).build()
        activeSubscription = createSubscription("activeSubscription", ACTIVE)

        monitoringProperties.getNumberOfThreads() >> 1
        monitoringProperties.getScanEvery() >> Duration.ofSeconds(100)

        topicService.listQualifiedTopicNames() >> List.of(topicName.qualifiedName())
        topicService.getTopicDetails(topicName) >> topic

        monitoringServicesCreator.createMonitoringServices() >> List.of(monitoringService)
    }

    def "Should start monitoring in constructor"() {
        given:
        monitoringProperties.isEnabled() >> true
        subscriptionService.listSubscriptions(topicName) >> List.of(activeSubscription)
        monitoringService.checkIfAllPartitionsAreAssigned(topic, activeSubscription.name) >> false

        when:
        def monitoringCache = new MonitoringCache(monitoringProperties, subscriptionService, topicService, monitoringServicesCreator)

        then:
        new PollingConditions().eventually {
            def result = monitoringCache.getSubscriptionsWithUnassignedPartitions()
            result.size() == 1
            result.first().subscription == activeSubscription.name
        }
    }

    def "Should return only one subscription with unassigned partitions"() {
        given:
        monitoringProperties.isEnabled() >> false
        def secondSubscriptionName = "secondSubscription"
        def secondSubscription = createSubscription(secondSubscriptionName, ACTIVE)
        subscriptionService.listSubscriptions(topicName) >> List.of(activeSubscription, secondSubscription)

        def monitoringCache = new MonitoringCache(monitoringProperties, subscriptionService, topicService, monitoringServicesCreator)

        when:
        monitoringCache.monitorSubscriptionsPartitions()
        def result = monitoringCache.getSubscriptionsWithUnassignedPartitions()

        then:
        result.size() == 1
        result.first().subscription == activeSubscription.name
        1 * monitoringService.checkIfAllPartitionsAreAssigned(topic, activeSubscription.name) >> false
        1 * monitoringService.checkIfAllPartitionsAreAssigned(topic, secondSubscriptionName) >> true
    }

    @Unroll
    def "Should ignore subscriptions with state #state"() {
        given:
        monitoringProperties.isEnabled() >> false
        def secondSubscriptionName = "secondSubscription"
        def secondSubscription = createSubscription(secondSubscriptionName, state)
        subscriptionService.listSubscriptions(topicName) >> List.of(activeSubscription, secondSubscription)

        def monitoringCache = new MonitoringCache(monitoringProperties, subscriptionService, topicService, monitoringServicesCreator)

        when:
        monitoringCache.monitorSubscriptionsPartitions()
        def result = monitoringCache.getSubscriptionsWithUnassignedPartitions()

        then:
        result.size() == 1
        result.first().subscription == activeSubscription.name
        1 * monitoringService.checkIfAllPartitionsAreAssigned(topic, activeSubscription.name) >> false
        0 * monitoringService.checkIfAllPartitionsAreAssigned(_ as Topic, _ as String)

        where:
        state << [PENDING, SUSPENDED]
    }

    private Subscription createSubscription(String name, Subscription.State state) {
        SubscriptionBuilder.subscription(topicName, name).withState(state).build()
    }
}