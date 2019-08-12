package pl.allegro.tech.hermes.consumers.subscription.id;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache;
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus;
import pl.allegro.tech.hermes.domain.notifications.SubscriptionCallback;

import java.util.Optional;

public class NotificationAwareSubscriptionIdsCache implements SubscriptionIds, SubscriptionCallback {

    private final SubscriptionsCache subscriptionsCache;
    private final SubscriptionIdProvider subscriptionIdProvider;
    private final BiMap<SubscriptionId, SubscriptionName> idsBiMap;

    public NotificationAwareSubscriptionIdsCache(InternalNotificationsBus notificationsBus,
                                                 SubscriptionsCache subscriptionsCache,
                                                 SubscriptionIdProvider subscriptionIdProvider) {
        this.subscriptionsCache = subscriptionsCache;
        this.subscriptionIdProvider = subscriptionIdProvider;

        notificationsBus.registerSubscriptionCallback(this);
        this.idsBiMap = Maps.synchronizedBiMap(HashBiMap.create());
    }

    @Override
    public void start() {
        subscriptionsCache.listActiveSubscriptionNames()
                .forEach(this::putSubscriptionId);
    }

    private void putSubscriptionId(SubscriptionName name) {
        idsBiMap.put(subscriptionIdProvider.getSubscriptionId(name), name);
    }

    @Override
    public Optional<SubscriptionId> getSubscriptionId(SubscriptionName subscriptionName) {
        return Optional.ofNullable(idsBiMap.inverse().get(subscriptionName));
    }

    @Override
    public Optional<SubscriptionName> getSubscriptionName(SubscriptionId subscriptionId) {
        return Optional.ofNullable(idsBiMap.get(subscriptionId));
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
        idsBiMap.inverse().remove(subscription.getQualifiedName());
    }
}
