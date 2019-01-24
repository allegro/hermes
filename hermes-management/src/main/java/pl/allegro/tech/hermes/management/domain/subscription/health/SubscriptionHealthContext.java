package pl.allegro.tech.hermes.management.domain.subscription.health;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionMetrics;
import pl.allegro.tech.hermes.api.TopicMetrics;

import static java.lang.Double.parseDouble;

public final class SubscriptionHealthContext {
    private final Subscription subscription;
    private final double topicRate;
    private final double subscriptionRate;
    private final double timeoutsRate;
    private final double otherErrorsRate;
    private final double code4xxErrorsRate;
    private final double code5xxErrorsRate;
    private final double batchRate;
    private final long lag;

    SubscriptionHealthContext(Subscription subscription, TopicMetrics topicMetrics, SubscriptionMetrics subscriptionMetrics) {
        this.subscription = subscription;
        this.topicRate = parseDouble(topicMetrics.getRate());
        this.subscriptionRate = parseDouble(subscriptionMetrics.getRate());
        this.timeoutsRate = parseDouble(subscriptionMetrics.getTimeouts());
        this.otherErrorsRate = parseDouble(subscriptionMetrics.getOtherErrors());
        this.code4xxErrorsRate = parseDouble(subscriptionMetrics.getCodes4xx());
        this.code5xxErrorsRate = parseDouble(subscriptionMetrics.getCodes5xx());
        this.batchRate = parseDouble(subscriptionMetrics.getBatchRate());
        this.lag = subscriptionMetrics.getLag();
    }

    public boolean subscriptionHasRetryOnError() {
        if (subscription.isBatchSubscription()) {
            return subscription.getBatchSubscriptionPolicy().isRetryClientErrors();
        } else {
            return subscription.getSerialSubscriptionPolicy().isRetryClientErrors();
        }
    }

    public double getSubscriptionRateRespectingDeliveryType() {
        if (subscription.isBatchSubscription()) {
            return batchRate;
        }
        return subscriptionRate;
    }

    public double getOtherErrorsRate() {
        return otherErrorsRate;
    }

    public double getTimeoutsRate() {
        return timeoutsRate;
    }

    public double getCode4xxErrorsRate() {
        return code4xxErrorsRate;
    }

    public double getCode5xxErrorsRate() {
        return code5xxErrorsRate;
    }

    public long getLag() {
        return lag;
    }

    public double getTopicRate() {
        return topicRate;
    }
}
