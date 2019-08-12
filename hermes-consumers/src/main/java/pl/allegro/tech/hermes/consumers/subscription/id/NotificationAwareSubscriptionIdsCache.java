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
    private final Map<SubscriptionName, SubscriptionId> nameToIdMap = new ConcurrentHashMap<>();
    private final Map<Long, SubscriptionId> valueToIdMap = new ConcurrentHashMap<>();

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
        SubscriptionId id = subscriptionIdProvider.getSubscriptionId(name);
        nameToIdMap.put(name, id);
        valueToIdMap.put(id.getValue(), id);
    }

    @Override
    public Optional<SubscriptionId> getSubscriptionId(SubscriptionName subscriptionName) {
        return Optional.ofNullable(nameToIdMap.get(subscriptionName));
    }

    @Override
    public Optional<SubscriptionId> getSubscriptionId(long id) {
        return Optional.ofNullable(valueToIdMap.get(id));
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
        Optional.ofNullable(nameToIdMap.remove(subscription.getQualifiedName()))
                .map(SubscriptionId::getValue)
                .ifPresent(valueToIdMap::remove);
    }
}
