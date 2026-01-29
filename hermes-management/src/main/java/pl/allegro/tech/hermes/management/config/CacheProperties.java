package pl.allegro.tech.hermes.management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cache")
public class CacheProperties {

  private int topicOwnerRefreshRateInSeconds = 60;
  private int subscriptionOwnerRefreshRateInSeconds = 60;

  public int getTopicOwnerRefreshRateInSeconds() {
    return topicOwnerRefreshRateInSeconds;
  }

  public void setTopicOwnerRefreshRateInSeconds(int topicOwnerRefreshRateInSeconds) {
    this.topicOwnerRefreshRateInSeconds = topicOwnerRefreshRateInSeconds;
  }

  public int getSubscriptionOwnerRefreshRateInSeconds() {
    return subscriptionOwnerRefreshRateInSeconds;
  }

  public void setSubscriptionOwnerRefreshRateInSeconds(int subscriptionOwnerRefreshRateInSeconds) {
    this.subscriptionOwnerRefreshRateInSeconds = subscriptionOwnerRefreshRateInSeconds;
  }
}
