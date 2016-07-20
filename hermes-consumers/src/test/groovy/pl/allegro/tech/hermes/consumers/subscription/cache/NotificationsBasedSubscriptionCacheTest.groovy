package pl.allegro.tech.hermes.consumers.subscription.cache

import pl.allegro.tech.hermes.api.Subscription
import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.api.TopicName
import pl.allegro.tech.hermes.domain.group.GroupRepository
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository
import pl.allegro.tech.hermes.domain.topic.TopicRepository
import pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder
import spock.lang.Specification

import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription

class NotificationsBasedSubscriptionCacheTest extends Specification {

    private InternalNotificationsBus notificationsBus = Stub(InternalNotificationsBus)

    private GroupRepository groupRepository = Stub(GroupRepository)

    private TopicRepository topicRepository = Stub(TopicRepository)

    private SubscriptionRepository subscriptionRepository = Stub(SubscriptionRepository)

    NotificationsBasedSubscriptionCache cache = new NotificationsBasedSubscriptionCache(
            notificationsBus,
            groupRepository,
            topicRepository,
            subscriptionRepository
    )

    def "should initialize cache on start"() {
        given:
        groupRepository.listGroupNames() >> ['group']
        topicRepository.listTopicNames('group') >> ['topic']
        subscriptionRepository.listSubscriptions(TopicName.fromQualifiedName('group.topic')) >> [subscription('group.topic', 'initial').build()]

        when:
        cache.start()

        then:
        cache.getSubscription(SubscriptionName.fromString('group.topic$initial'))
    }

    def "should return only active subscriptions"() {
        given:
        cache.onSubscriptionCreated(subscription('group.topic', 'active').withState(Subscription.State.ACTIVE).build())
        cache.onSubscriptionCreated(subscription('group.topic', 'inactive').withState(Subscription.State.SUSPENDED).build())

        expect:
        cache.listActiveSubscriptionNames() == [SubscriptionName.fromString('group.topic$active')]
    }

    def "should return subscription by name"() {
        given:
        cache.onSubscriptionCreated(subscription('group.topic', 'sub').build())

        expect:
        cache.getSubscription(SubscriptionName.fromString('group.topic$sub')).name == 'sub'
    }

}
