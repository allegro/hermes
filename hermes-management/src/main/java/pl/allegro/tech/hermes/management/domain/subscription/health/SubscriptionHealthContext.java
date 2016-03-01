package pl.allegro.tech.hermes.management.domain.subscription.health;

import pl.allegro.tech.hermes.api.Subscription;

public final class SubscriptionHealthContext {
    private final Subscription subscription;
    private final TopicMetrics topicMetrics;
    private final SubscriptionMetrics subscriptionMetrics;

    public SubscriptionHealthContext(Subscription subscription, TopicMetrics topicMetrics, SubscriptionMetrics subscriptionMetrics) {
        this.subscription = subscription;
        this.topicMetrics = topicMetrics;
        this.subscriptionMetrics = subscriptionMetrics;
    }

    public Subscription getSubscription() {
        return subscription;
    }

    public TopicMetrics getTopicMetrics() {
        return topicMetrics;
    }

    public SubscriptionMetrics getSubscriptionMetrics() {
        return subscriptionMetrics;
    }
}
