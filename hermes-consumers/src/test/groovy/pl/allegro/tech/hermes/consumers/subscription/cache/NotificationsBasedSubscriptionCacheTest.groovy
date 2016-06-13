package pl.allegro.tech.hermes.consumers.subscription.cache

import pl.allegro.tech.hermes.api.Subscription
import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus
import spock.lang.Specification

import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription

class NotificationsBasedSubscriptionCacheTest extends Specification {

    InternalNotificationsBus notificationsBus = Mock(InternalNotificationsBus)

    NotificationsBasedSubscriptionCache cache = new NotificationsBasedSubscriptionCache(notificationsBus)

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
