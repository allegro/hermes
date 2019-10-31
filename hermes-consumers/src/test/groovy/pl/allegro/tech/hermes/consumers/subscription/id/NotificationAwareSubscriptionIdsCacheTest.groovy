package pl.allegro.tech.hermes.consumers.subscription.id


import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus
import pl.allegro.tech.hermes.test.helper.cache.FakeTicker
import spock.lang.Shared
import spock.lang.Specification

import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription

class NotificationAwareSubscriptionIdsCacheTest extends Specification {

    @Shared
    private SubscriptionNames subscriptionNames = new SubscriptionNames()

    private InternalNotificationsBus notificationsBus = Mock(InternalNotificationsBus)
    private SubscriptionsCache subscriptionCache = Stub(SubscriptionsCache)

    private SubscriptionIdProvider subscriptionIdProvider = Stub(SubscriptionIdProvider)

    NotificationAwareSubscriptionIdsCache subscriptionIds

    @Shared
    private FakeTicker ticker = new FakeTicker()

    def setup() {
        def evictAfterSeconds = 30
        subscriptionIds = new NotificationAwareSubscriptionIdsCache(notificationsBus, subscriptionCache,
                subscriptionIdProvider, evictAfterSeconds, ticker)
    }

    def "should match subscription by id"() {
        given:
        def sub1 = subscriptionNames.next()
        def sub2 = subscriptionNames.next()
        def sub3 = subscriptionNames.next()

        def id1 = SubscriptionId.from(sub1, 1L)
        def id2 = SubscriptionId.from(sub2, 2L)
        def id3 = SubscriptionId.from(sub3, 3L)

        subscriptionIdProvider.getSubscriptionId(sub1) >> id1
        subscriptionIdProvider.getSubscriptionId(sub2) >> id2
        subscriptionIdProvider.getSubscriptionId(sub3) >> id3

        subscriptionCache.listActiveSubscriptionNames() >> [sub1, sub2]

        when:
        subscriptionIds.start()

        then:
        subscriptionIds.getSubscriptionId(sub1).get() == id1
        subscriptionIds.getSubscriptionId(id1.value).get() == id1

        subscriptionIds.getSubscriptionId(sub2).get() == id2
        subscriptionIds.getSubscriptionId(id2.value).get() == id2

        !subscriptionIds.getSubscriptionId(sub3).isPresent()
        !subscriptionIds.getSubscriptionId(id3.value).isPresent()
    }

    def "should handle subscription callback zk events"() {
        given:
        def sub1 = subscriptionNames.next()
        def sub2 = subscriptionNames.next()

        def id1 = SubscriptionId.from(sub1, 1L)
        def id2 = SubscriptionId.from(sub2, 2L)

        subscriptionIdProvider.getSubscriptionId(sub1) >> id1
        subscriptionIdProvider.getSubscriptionId(sub2) >> id2

        subscriptionCache.listActiveSubscriptionNames() >> []

        when:
        subscriptionIds.start()

        then:
        !subscriptionIds.getSubscriptionId(sub1).isPresent()
        !subscriptionIds.getSubscriptionId(sub2).isPresent()

        when:
        subscriptionIds.onSubscriptionCreated(subscription(sub1).build())

        then:
        subscriptionIds.getSubscriptionId(sub1).get() == id1
        subscriptionIds.getSubscriptionId(id1.value).get() == id1
        !subscriptionIds.getSubscriptionId(sub2).isPresent()
        !subscriptionIds.getSubscriptionId(id2.value).isPresent()

        when:
        subscriptionIds.onSubscriptionChanged(subscription(sub2).build())

        then:
        subscriptionIds.getSubscriptionId(sub2).get() == id2
        subscriptionIds.getSubscriptionId(id2.value).get() == id2

        when:
        subscriptionIds.onSubscriptionRemoved(subscription(sub1).build())

        then:
        subscriptionIds.getSubscriptionId(sub1).isPresent()
        subscriptionIds.getSubscriptionId(id1.value).isPresent()
        subscriptionIds.getSubscriptionId(sub2).get() == id2
        subscriptionIds.getSubscriptionId(id2.value).get() == id2
    }

    def "should evict removed subscription ids after specified time"() {
        given:
        def sub1 = subscriptionNames.next()
        def sub2 = subscriptionNames.next()

        def id1 = SubscriptionId.from(sub1, 1L)
        def id2 = SubscriptionId.from(sub2, 2L)

        subscriptionIdProvider.getSubscriptionId(sub1) >> id1
        subscriptionIdProvider.getSubscriptionId(sub2) >> id2

        subscriptionCache.listActiveSubscriptionNames() >> [sub1, sub2]

        when:
        subscriptionIds.start()

        then:
        subscriptionIds.getSubscriptionId(sub1).isPresent()
        subscriptionIds.getSubscriptionId(sub2).isPresent()
        subscriptionIds.getSubscriptionId(id1.value).isPresent()
        subscriptionIds.getSubscriptionId(id2.value).isPresent()

        when:
        subscriptionIds.onSubscriptionRemoved(subscription(sub1).build())
        ticker.advance(Duration.ofSeconds(20))

        then:
        subscriptionIds.getSubscriptionId(sub1).isPresent() // still available
        subscriptionIds.getSubscriptionId(id1.value).isPresent()
        subscriptionIds.getSubscriptionId(sub2).isPresent()
        subscriptionIds.getSubscriptionId(id2.value).isPresent()

        when:
        ticker.advance(Duration.ofSeconds(30))

        then:
        !subscriptionIds.getSubscriptionId(sub1).isPresent() // expires
        !subscriptionIds.getSubscriptionId(id1.value).isPresent()
        subscriptionIds.getSubscriptionId(sub2).isPresent()
        subscriptionIds.getSubscriptionId(id2.value).isPresent()
    }

    private static class SubscriptionNames {
        def count = new AtomicInteger()

        SubscriptionName next() {
            return SubscriptionName.fromString("pl.allegro.tech.consumers\$${count.incrementAndGet()}")
        }
    }
}
