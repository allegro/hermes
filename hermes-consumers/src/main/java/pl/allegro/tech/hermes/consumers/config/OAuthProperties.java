package pl.allegro.tech.hermes.consumers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "consumer.oauth")
public class OAuthProperties {

    private long missingSubscriptionHandlersCreationDelay = 10_000L;

    private long subscriptionTokensCacheMaxSize = 1000L;

    private double providersTokenRequestRateLimiterRateReductionFactor = 2.0;

    public long getMissingSubscriptionHandlersCreationDelay() {
        return missingSubscriptionHandlersCreationDelay;
    }

    public void setMissingSubscriptionHandlersCreationDelay(long missingSubscriptionHandlersCreationDelay) {
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

    public void setProvidersTokenRequestRateLimiterRateReductionFactor(double providersTokenRequestRateLimiterRateReductionFactor) {
        this.providersTokenRequestRateLimiterRateReductionFactor = providersTokenRequestRateLimiterRateReductionFactor;
    }
}
