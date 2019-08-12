package pl.allegro.tech.hermes.consumers.subscription.id;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus;
import pl.allegro.tech.hermes.domain.notifications.SubscriptionCallback;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class NotificationAwareSubscriptionIdsCache implements SubscriptionIds, SubscriptionCallback {

    private final SubscriptionsCache subscriptionsCache;
    private final SubscriptionIdProvider subscriptionIdProvider;
    private final Map<SubscriptionName, SubscriptionId> ids = new ConcurrentHashMap<>();

    public NotificationAwareSubscriptionIdsCache(InternalNotificationsBus notificationsBus,
                                                 SubscriptionsCache subscriptionsCache,
                                                 SubscriptionIdProvider subscriptionIdProvider) {
        this.subscriptionsCache = subscriptionsCache;
        this.subscriptionIdProvider = subscriptionIdProvider;

        notificationsBus.registerSubscriptionCallback(this);
    }

    @Override
    public void start() {
        subscriptionsCache.listActiveSubscriptionNames()
                .forEach(this::putSubscriptionId);
    }

    private void putSubscriptionId(SubscriptionName name) {
        ids.put(name, subscriptionIdProvider.getSubscriptionId(name));
    }

    @Override
    public Optional<SubscriptionId> getSubscriptionId(SubscriptionName subscriptionName) {
        return Optional.ofNullable(ids.get(subscriptionName));
    }

    @Override
    public void onSubscriptionCreated(Subscription subscription) {
        putSubscriptionId(subscription.getQualifiedName());
    }

    @Override
    public void onSubscriptionChanged(Subscription subscription) {
        putSubscriptionId(subscription.getQualifiedName());
    }

    @Override
    public void onSubscriptionRemoved(Subscription subscription) {
        ids.remove(subscription.getQualifiedName());
    }
}
