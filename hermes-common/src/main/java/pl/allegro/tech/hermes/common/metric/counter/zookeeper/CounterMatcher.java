package pl.allegro.tech.hermes.common.metric.counter.zookeeper;

import static pl.allegro.tech.hermes.common.metric.SubscriptionMetrics.SubscriptionMetricsNames;
import static pl.allegro.tech.hermes.common.metric.TopicMetrics.TopicMetricsNames;

import io.micrometer.core.instrument.Counter;
import java.util.Optional;
import pl.allegro.tech.hermes.api.TopicName;

class CounterMatcher {

  private static final String GROUP_TAG_NAME = "group";
  private static final String TOPIC_TAG_NAME = "topic";
  private static final String SUBSCRIPTION_TAG_NAME = "subscription";

  private final Counter counter;
  private final String metricSearchPrefix;
  private TopicName topicName;
  private long value;
  private Optional<String> subscription;

  public CounterMatcher(Counter counter, String metricSearchPrefix) {
    this.counter = counter;
    this.metricSearchPrefix = metricSearchPrefix;
    parseCounter(this.counter);
  }

  private void parseCounter(Counter counter) {
    if (isTopicPublished() || isTopicThroughput()) {
      topicName =
          new TopicName(
              counter.getId().getTag(GROUP_TAG_NAME), counter.getId().getTag(TOPIC_TAG_NAME));
      subscription = Optional.empty();
    } else if (isSubscriptionDelivered()
        || isSubscriptionThroughput()
        || isSubscriptionDiscarded()
        || isSubscriptionFiltered()) {
      topicName =
          new TopicName(
              counter.getId().getTag(GROUP_TAG_NAME), counter.getId().getTag(TOPIC_TAG_NAME));
      subscription = Optional.of(counter.getId().getTag(SUBSCRIPTION_TAG_NAME));
    }
    value = (long) counter.count();
  }

  public boolean isTopicPublished() {
    return isTopicCounter() && nameEquals(TopicMetricsNames.TOPIC_PUBLISHED);
  }

  public boolean isTopicThroughput() {
    return isTopicCounter() && nameEquals(TopicMetricsNames.TOPIC_THROUGHPUT);
  }

  public boolean isSubscriptionThroughput() {
    return isSubscriptionCounter() && nameEquals(SubscriptionMetricsNames.SUBSCRIPTION_THROUGHPUT);
  }

  public boolean isSubscriptionDelivered() {
    return isSubscriptionCounter() && nameEquals(SubscriptionMetricsNames.SUBSCRIPTION_DELIVERED);
  }

  public boolean isSubscriptionDiscarded() {
    return isSubscriptionCounter() && nameEquals(SubscriptionMetricsNames.SUBSCRIPTION_DISCARDED);
  }

  public boolean isSubscriptionFiltered() {
    return isSubscriptionCounter()
        && nameEquals(SubscriptionMetricsNames.SUBSCRIPTION_FILTERED_OUT);
  }

  public TopicName getTopicName() {
    return topicName;
  }

  public String getSubscriptionName() {
    return subscription.orElse("");
  }

  public long getValue() {
    return value;
  }

  private boolean isTopicCounter() {
    return counter.getId().getTag(TOPIC_TAG_NAME) != null;
  }

  private boolean isSubscriptionCounter() {
    return counter.getId().getTag(SUBSCRIPTION_TAG_NAME) != null;
  }

  private boolean nameEquals(String name) {
    return counter.getId().getName().equals(metricSearchPrefix + name);
  }
}
