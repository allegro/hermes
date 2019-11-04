package pl.allegro.tech.hermes.consumers.subscription.id;

import com.google.common.base.Ticker;
import org.glassfish.hk2.api.Factory;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus;

import javax.inject.Inject;

public class SubscriptionIdsCacheFactory implements Factory<SubscriptionIds> {

    private final InternalNotificationsBus internalNotificationsBus;
    private final SubscriptionsCache subscriptionsCache;
    private final SubscriptionIdProvider subscriptionIdProvider;
    private final ConfigFactory configFactory;

    @Inject
    public SubscriptionIdsCacheFactory(InternalNotificationsBus internalNotificationsBus,
                                       SubscriptionsCache subscriptionsCache,
                                       SubscriptionIdProvider subscriptionIdProvider,
                                       ConfigFactory configFactory) {
        this.internalNotificationsBus = internalNotificationsBus;
        this.subscriptionsCache = subscriptionsCache;
        this.subscriptionIdProvider = subscriptionIdProvider;
        this.configFactory = configFactory;
    }

    @Override
    public SubscriptionIds provide() {
        long removedSubscriptionsExpireAfterAccessSeconds = configFactory.getLongProperty(Configs.CONSUMER_SUBSCRIPTION_IDS_CACHE_REMOVED_EXPIRE_AFTER_ACCESS_SECONDS);
        NotificationAwareSubscriptionIdsCache cache = new NotificationAwareSubscriptionIdsCache(
                internalNotificationsBus, subscriptionsCache, subscriptionIdProvider, removedSubscriptionsExpireAfterAccessSeconds, Ticker.systemTicker()
        );
        cache.start();
        return cache;
    }

    @Override
    public void dispose(SubscriptionIds instance) {
    }
}
