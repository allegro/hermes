package pl.allegro.tech.hermes.consumers.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "consumer.oauth")
public class OAuthProperties {

  private Duration missingSubscriptionHandlersCreationDelay = Duration.ofSeconds(10);

  private long subscriptionTokensCacheMaxSize = 1000L;

  private double providersTokenRequestRateLimiterRateReductionFactor = 2.0;

  public Duration getMissingSubscriptionHandlersCreationDelay() {
    return missingSubscriptionHandlersCreationDelay;
  }

  public void setMissingSubscriptionHandlersCreationDelay(
      Duration missingSubscriptionHandlersCreationDelay) {
    this.missingSubscriptionHandlersCreationDelay = missingSubscriptionHandlersCreationDelay;
  }

  public long getSubscriptionTokensCacheMaxSize() {
    return subscriptionTokensCacheMaxSize;
  }

  public void setSubscriptionTokensCacheMaxSize(long subscriptionTokensCacheMaxSize) {
    this.subscriptionTokensCacheMaxSize = subscriptionTokensCacheMaxSize;
  }

  public double getProvidersTokenRequestRateLimiterRateReductionFactor() {
    return providersTokenRequestRateLimiterRateReductionFactor;
  }

  public void setProvidersTokenRequestRateLimiterRateReductionFactor(
      double providersTokenRequestRateLimiterRateReductionFactor) {
    this.providersTokenRequestRateLimiterRateReductionFactor =
        providersTokenRequestRateLimiterRateReductionFactor;
  }
}
