package pl.allegro.tech.hermes.consumers.subscription.id

import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus
import spock.lang.Shared
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicInteger

import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription

class NotificationAwareSubscriptionIdsCacheTest extends Specification {

    @Shared
    private SubscriptionNames subscriptionNames = new SubscriptionNames()

    private InternalNotificationsBus notificationsBus = Stub(InternalNotificationsBus)

    private SubscriptionsCache subscriptionCache = Stub(SubscriptionsCache)

    def subscriptionIds = new NotificationAwareSubscriptionIdsCache(notificationsBus, subscriptionCache)

    def "should match subscription by id"() {
        given:
        def sub1 = subscriptionNames.next()
        def sub2 = subscriptionNames.next()
        def sub3 = subscriptionNames.next()
        subscriptionCache.listActiveSubscriptionNames() >> [sub1, sub2]

        when:
        subscriptionIds.start()

        then:
        subscriptionIds.getSubscriptionName(SubscriptionId.of(sub1)).get() == sub1
        subscriptionIds.getSubscriptionName(SubscriptionId.of(sub2)).get() == sub2
        !subscriptionIds.getSubscriptionName(SubscriptionId.of(sub3)).isPresent()
    }

    def "should compute subscription ids"() {
        when:
        def sub1 = subscriptionNames.next()

        then:
        subscriptionIds.getSubscriptionId(sub1) == SubscriptionId.of(sub1)
    }

    def "should handle subscription callback zk events"() {
        given:
        def sub1 = subscriptionNames.next()
        def sub2 = subscriptionNames.next()
        def sub1Id = SubscriptionId.of(sub1)
        def sub2Id = SubscriptionId.of(sub2)
        subscriptionCache.listActiveSubscriptionNames() >> []

        when:
        subscriptionIds.start()

        then:
        !subscriptionIds.getSubscriptionName(sub1Id).ifPresent()
        !subscriptionIds.getSubscriptionName(sub2Id).ifPresent()

        when:
        subscriptionIds.onSubscriptionCreated(subscription(sub1).build())

        then:
        subscriptionIds.getSubscriptionName(sub1Id).get() == sub1
        !subscriptionIds.getSubscriptionName(sub2Id).ifPresent()

        when:
        subscriptionIds.onSubscriptionChanged(subscription(sub2).build())

        then:
        subscriptionIds.getSubscriptionName(sub2Id).get() == sub2
        subscriptionIds.getSubscriptionName(sub1Id).get() == sub1

        when:
        subscriptionIds.onSubscriptionRemoved(subscription(sub1).build())

        then:
        !subscriptionIds.getSubscriptionName(sub1Id).isPresent()
        subscriptionIds.getSubscriptionName(sub2Id).get() == sub2
    }

    private static class SubscriptionNames {
        def count = new AtomicInteger()

        SubscriptionName next() {
            return SubscriptionName.fromString("pl.allegro.tech.consumers\$${count.incrementAndGet()}")
        }
    }
}
