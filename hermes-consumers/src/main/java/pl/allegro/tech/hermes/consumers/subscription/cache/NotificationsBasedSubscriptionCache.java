package pl.allegro.tech.hermes.consumers.subscription.cache;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.domain.notifications.InternalNotificationsBus;
import pl.allegro.tech.hermes.domain.notifications.SubscriptionCallback;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class NotificationsBasedSubscriptionCache implements SubscriptionsCache, SubscriptionCallback {

    private final Map<SubscriptionName, Subscription> subscriptions = new ConcurrentHashMap<>();

    @Inject
    public NotificationsBasedSubscriptionCache(InternalNotificationsBus notificationsBus) {
        notificationsBus.registerSubscriptionCallback(this);
    }

    @Override
    public void onSubscriptionCreated(Subscription subscription) {
        this.subscriptions.put(subscription.toSubscriptionName(), subscription);
    }

    @Override
    public void onSubscriptionRemoved(Subscription subscription) {
        this.subscriptions.remove(subscription.toSubscriptionName(), subscription);
    }

    @Override
    public void onSubscriptionChanged(Subscription subscription) {
        this.subscriptions.put(subscription.toSubscriptionName(), subscription);
    }

    @Override
    public Subscription getSubscription(SubscriptionName subscriptionName) {
        return subscriptions.get(subscriptionName);
    }

    @Override
    public List<Subscription> subscriptionsOfTopic(TopicName topicName) {
        return subscriptions.values().stream()
                .filter(s -> s.getTopicName().equals(topicName))
                .collect(Collectors.toList());
    }

    @Override
    public List<SubscriptionName> listActiveSubscriptionNames() {
        return subscriptions.values().stream()
                .filter(Subscription::isActive)
                .map(Subscription::toSubscriptionName)
                .collect(Collectors.toList());
    }
}
