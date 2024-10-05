package pl.allegro.tech.hermes.management.domain.subscription.health;

import java.util.Optional;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionMetrics;
import pl.allegro.tech.hermes.api.TopicMetrics;

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

  private SubscriptionHealthContext(
      Subscription subscription,
      TopicMetrics topicMetrics,
      SubscriptionMetrics subscriptionMetrics) {
    this.subscription = subscription;
    this.topicRate = topicMetrics.getRate().toDouble();
    this.subscriptionRate = subscriptionMetrics.getRate().toDouble();
    this.timeoutsRate = subscriptionMetrics.getTimeouts().toDouble();
    this.otherErrorsRate = subscriptionMetrics.getOtherErrors().toDouble();
    this.code4xxErrorsRate = subscriptionMetrics.getCodes4xx().toDouble();
    this.code5xxErrorsRate = subscriptionMetrics.getCodes5xx().toDouble();
    this.batchRate = subscriptionMetrics.getBatchRate().toDouble();
    this.lag = subscriptionMetrics.getLag().toLong();
  }

  static Optional<SubscriptionHealthContext> createIfAllMetricsExist(
      Subscription subscription,
      TopicMetrics topicMetrics,
      SubscriptionMetrics subscriptionMetrics) {
    if (topicMetrics.getRate().isAvailable()
        && subscriptionMetrics.getRate().isAvailable()
        && subscriptionMetrics.getTimeouts().isAvailable()
        && subscriptionMetrics.getOtherErrors().isAvailable()
        && subscriptionMetrics.getCodes4xx().isAvailable()
        && subscriptionMetrics.getCodes5xx().isAvailable()
        && subscriptionMetrics.getBatchRate().isAvailable()
        && subscriptionMetrics.getLag().isAvailable()) {
      return Optional.of(
          new SubscriptionHealthContext(subscription, topicMetrics, subscriptionMetrics));
    }
    return Optional.empty();
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

  public Subscription getSubscription() {
    return subscription;
  }
}
