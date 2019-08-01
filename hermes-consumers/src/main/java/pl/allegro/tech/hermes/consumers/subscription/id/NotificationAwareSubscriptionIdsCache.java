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

    private final Map<SubscriptionId, SubscriptionName> ids = new ConcurrentHashMap<>();
    private final SubscriptionsCache subscriptionsCache;

    public NotificationAwareSubscriptionIdsCache(InternalNotificationsBus notificationsBus,
                                                 SubscriptionsCache subscriptionsCache) {
        this.subscriptionsCache = subscriptionsCache;
        notificationsBus.registerSubscriptionCallback(this);
    }

    @Override
    public void start() {
        subscriptionsCache.listActiveSubscriptionNames()
                .forEach(this::putSubscriptionId);
    }

    private void putSubscriptionId(SubscriptionName name) {
        ids.put(SubscriptionId.of(name), name);
    }

    @Override
    public SubscriptionId getSubscriptionId(SubscriptionName subscriptionName) {
        return SubscriptionId.of(subscriptionName);
    }

    @Override
    public Optional<SubscriptionName> getSubscriptionName(SubscriptionId subscriptionId) {
        return Optional.ofNullable(ids.get(subscriptionId));
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
        ids.remove(SubscriptionId.of(subscription.getQualifiedName()));
    }
}
