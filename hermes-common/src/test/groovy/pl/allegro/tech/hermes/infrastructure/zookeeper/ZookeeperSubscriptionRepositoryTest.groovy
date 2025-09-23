package pl.allegro.tech.hermes.infrastructure.zookeeper

import pl.allegro.tech.hermes.api.EndpointAddress
import pl.allegro.tech.hermes.api.Group
import pl.allegro.tech.hermes.api.Subscription
import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.api.helpers.Patch
import pl.allegro.tech.hermes.domain.subscription.SubscriptionNotExistsException
import pl.allegro.tech.hermes.domain.topic.TopicNotExistsException
import pl.allegro.tech.hermes.infrastructure.MalformedDataException
import pl.allegro.tech.hermes.test.IntegrationTest

import java.time.Instant
import java.time.temporal.ChronoUnit

import static pl.allegro.tech.hermes.api.PatchData.patchData
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic

class ZookeeperSubscriptionRepositoryTest extends IntegrationTest {

    private static final String GROUP = "subscriptionRepositoryGroup"

    private static final TopicName TOPIC = new TopicName(GROUP, 'topic')

    private ZookeeperSubscriptionRepository repository = new ZookeeperSubscriptionRepository(zookeeper(), mapper, paths, topicRepository)

    void setup() {
        if (!groupRepository.groupExists(GROUP)) {
            groupRepository.createGroup(Group.from(GROUP))
            topicRepository.createTopic(topic(TOPIC).build())
        }
    }

    def "should create subscription"() {
        given:
        repository.createSubscription(subscription(TOPIC, 'create').build())
        wait.untilSubscriptionCreated(TOPIC, 'create')

        expect:
        repository.listSubscriptionNames(TOPIC).contains('create')
    }

    def "should throw exception when trying to add subscription to unknonw topic"() {
        when:
        repository.createSubscription(subscription("${GROUP}.unknown", 'unknown').build())

        then:
        thrown(TopicNotExistsException)
    }

    def "should return names of all defined subscriptions"() {
        given:
        repository.createSubscription(subscription(TOPIC, 'listNames1').build())
        repository.createSubscription(subscription(TOPIC, 'listNames2').build())
        wait.untilSubscriptionCreated(TOPIC, 'listNames1')
        wait.untilSubscriptionCreated(TOPIC, 'listNames2')

        expect:
        repository.listSubscriptionNames(TOPIC).containsAll('listNames1', 'listNames2')
    }

    def "should return true when subscription exists"() {
        given:
        repository.createSubscription(subscription(TOPIC, 'exists').build())
        wait.untilSubscriptionCreated(TOPIC, 'exists')

        expect:
        repository.subscriptionExists(TOPIC, 'exists')
    }

    def "should return false when subscription does no exist"() {
        expect:
        !repository.subscriptionExists(TOPIC, 'unknown')
    }

    def "should throw exception when subscription does not exist"() {
        when:
        repository.ensureSubscriptionExists(TOPIC, 'unknown')

        then:
        thrown(SubscriptionNotExistsException)
    }

    def "should return subscription details"() {
        given:
        def timestamp = Instant.now()
        repository.createSubscription(subscription(TOPIC, 'details', EndpointAddress.of('hello'))
                .withDescription('my description')
                .build())
        wait.untilSubscriptionCreated(TOPIC, 'details')

        when:
        Subscription subscription = repository.getSubscriptionDetails(TOPIC, 'details')

        then:
        subscription.description == 'my description'
        subscription.endpoint == EndpointAddress.of('hello')

        and: 'createdAt and modifiedAt are greater or equal than timestamp'
        subscription.createdAt.isAfter(timestamp)
        subscription.modifiedAt.isAfter(timestamp)
    }

    def "should throw exception when trying to return details of unknown subscription"() {
        when:
        repository.getSubscriptionDetails(TOPIC, 'unknown')

        then:
        thrown(SubscriptionNotExistsException)
    }

    def "should return details of topics subscriptions"() {
        given:
        Subscription subscription1 = subscription(TOPIC, 'list1').build()
        Subscription subscription2 = subscription(TOPIC, 'list2').build()
        repository.createSubscription(subscription1)
        repository.createSubscription(subscription2)
        wait.untilSubscriptionCreated(TOPIC, 'list1')
        wait.untilSubscriptionCreated(TOPIC, 'list2')

        expect:
        repository.listSubscriptions(TOPIC).containsAll(subscription1, subscription2)
    }

    def "should remove subscription"() {
        given:
        repository.createSubscription(subscription(TOPIC, 'remove').build())
        wait.untilSubscriptionCreated(TOPIC, 'remove')

        when:
        repository.removeSubscription(TOPIC, 'remove')

        then:
        !repository.subscriptionExists(TOPIC, 'remove')
    }

    def "should change subscription state"() {
        given:
        repository.createSubscription(subscription(TOPIC, 'state').build())
        wait.untilSubscriptionCreated(TOPIC, 'state')

        when:
        repository.updateSubscriptionState(TOPIC, 'state', Subscription.State.SUSPENDED)

        then:
        repository.getSubscriptionDetails(TOPIC, 'state').state == Subscription.State.SUSPENDED
    }

    def "should change subscription endpoint"() {
        given:
        def retrieved = subscription(TOPIC, 'endpoint', EndpointAddress.of("http://localhost:8080/v1")).build()

        repository.createSubscription(retrieved)
        wait.untilSubscriptionCreated(TOPIC, 'endpoint')

        def updated = Patch.apply(retrieved, patchData().set("endpoint", EndpointAddress.of("http://localhost:8080/v2")).build());

        when:
        repository.updateSubscription(updated)

        then:
        repository.getSubscriptionDetails(TOPIC, 'endpoint').endpoint == EndpointAddress.of("http://localhost:8080/v2")
    }

    def "should not throw exception on malformed topic when reading list of all topics"() {
        given:
        zookeeper().create().forPath(paths.subscriptionPath(TOPIC, 'malformed'), ''.bytes)
        wait.untilSubscriptionCreated(TOPIC, 'malformed')

        when:
        List<Subscription> subscriptions = repository.listSubscriptions(TOPIC)

        then:
        notThrown(MalformedDataException)
    }

    def "should get list of subscriptions based on names list"() {
        given:
        /*
            When we retrieve subscriptions from our ZooKeeper repository, their timestamps (createdAt and modifiedAt) are
            set based on ZooKeeper's internal node stats. This causes them to differ from the timestamps on the original objects we created.
            Since our equals() and hashCode() methods rely on all fields, including these timestamps, a retrieved subscription
            will not be considered equal to the original one. To ensure our test assertions pass, we must override the retrieved
            timestamps to match the original ones. This workaround allows us to confirm that the object retrieved is an exact
            match for the object we initially persisted.
         */
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS)
        def subscription1 = subscription(TOPIC, 'subscription1', EndpointAddress.of('hello'))
                .withCreatedAt(now)
                .withModifiedAt(now).build()
        def subscription2 = subscription(TOPIC, 'subscription2', EndpointAddress.of('hello'))
                .withCreatedAt(now)
                .withModifiedAt(now)
                .build()
        def subscriptionName1 = new SubscriptionName("subscription1", TOPIC)
        def subscriptionName2 = new SubscriptionName("subscription2", TOPIC)

        repository.createSubscription(subscription1)
        repository.createSubscription(subscription2)

        wait.untilSubscriptionCreated(TOPIC, 'subscription1')
        wait.untilSubscriptionCreated(TOPIC, 'subscription2')

        when:
        List<Subscription> retrieved = repository.getSubscriptionDetails([subscriptionName1, subscriptionName2])
        retrieved.forEach {
            // Set timestamps to match the ones we created before persisting.
            // The retrieved subscriptions will have timestamps set based on zookeeper node stats.
            it.setCreatedAt(now.toEpochMilli())
            it.setModifiedAt(now.toEpochMilli())
        }

        then:
        retrieved.containsAll([subscription1, subscription2])
    }
}
