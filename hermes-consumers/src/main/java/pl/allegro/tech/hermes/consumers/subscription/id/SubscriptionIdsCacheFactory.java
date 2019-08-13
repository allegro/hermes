package pl.allegro.tech.hermes.consumers.subscription.id;

import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus;

import javax.inject.Inject;

public class SubscriptionIdsCacheFactory implements Factory<SubscriptionIds> {

    private final InternalNotificationsBus internalNotificationsBus;
    private final SubscriptionsCache subscriptionsCache;
    private final SubscriptionIdProvider subscriptionIdProvider;

    @Inject
    public SubscriptionIdsCacheFactory(InternalNotificationsBus internalNotificationsBus,
                                       SubscriptionsCache subscriptionsCache,
                                       SubscriptionIdProvider subscriptionIdProvider) {
        this.internalNotificationsBus = internalNotificationsBus;
        this.subscriptionsCache = subscriptionsCache;
        this.subscriptionIdProvider = subscriptionIdProvider;
    }

    @Override
    public SubscriptionIds provide() {
        NotificationAwareSubscriptionIdsCache cache = new NotificationAwareSubscriptionIdsCache(
                internalNotificationsBus, subscriptionsCache, subscriptionIdProvider);
        cache.start();
        return cache;
    }

    @Override
    public void dispose(SubscriptionIds instance) {
    }
}
