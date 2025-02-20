package pl.allegro.tech.hermes.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public class Stats {
  private final TopicStats topicStats;
  private final SubscriptionStats subscriptionStats;

  @JsonCreator
  public Stats(
      @JsonProperty("topicStats") TopicStats topicStats,
      @JsonProperty("subscriptionStats") SubscriptionStats subscriptionStats) {
    this.topicStats = topicStats;
    this.subscriptionStats = subscriptionStats;
  }

  public TopicStats getTopicStats() {
    return topicStats;
  }

  public SubscriptionStats getSubscriptionStats() {
    return subscriptionStats;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Stats stats = (Stats) o;
    return Objects.equals(topicStats, stats.topicStats)
        && Objects.equals(subscriptionStats, stats.subscriptionStats);
  }

  @Override
  public int hashCode() {
    return Objects.hash(topicStats, subscriptionStats);
  }

  @Override
  public String toString() {
    return "Stats{" + "topicStats=" + topicStats + ", subscriptionStats=" + subscriptionStats + '}';
  }
}
