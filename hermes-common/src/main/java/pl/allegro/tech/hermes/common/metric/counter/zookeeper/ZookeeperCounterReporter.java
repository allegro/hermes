package pl.allegro.tech.hermes.common.metric.counter.zookeeper;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.metric.counter.CounterStorage;
import pl.allegro.tech.hermes.metrics.PathsCompiler;

public class ZookeeperCounterReporter {
  private static final Logger logger = LoggerFactory.getLogger(ZookeeperCounterReporter.class);

  private final CounterStorage counterStorage;
  private final String metricsSearchPrefix;
  private final MeterRegistry meterRegistry;

  private final ScheduledExecutorService scheduledExecutorService =
      Executors.newSingleThreadScheduledExecutor(
          new ThreadFactoryBuilder()
              .setNameFormat("zookeeper-reporter-scheduled-executor-%d")
              .setDaemon(true)
              .build());

  public ZookeeperCounterReporter(
      MeterRegistry registry, CounterStorage counterStorage, String metricsSearchPrefix) {
    this.meterRegistry = registry;
    this.counterStorage = counterStorage;
    this.metricsSearchPrefix = metricsSearchPrefix;
  }

  public void start(long period, TimeUnit unit) {
    scheduledExecutorService.scheduleWithFixedDelay(this::report, 0, period, unit);
  }

  public void report() {
    try {
      Collection<Counter> counters = Search.in(meterRegistry).counters();
      counters.forEach(
          counter -> {
            CounterMatcher matcher = new CounterMatcher(counter, metricsSearchPrefix);
            reportCounter(matcher);
            reportVolumeCounter(matcher);
          });
    } catch (RuntimeException ex) {
      logger.error("Error during reporting metrics to Zookeeper...", ex);
    }
  }

  private void reportVolumeCounter(CounterMatcher matcher) {
    if (matcher.isTopicThroughput()) {
      counterStorage.incrementVolumeCounter(
          escapedTopicName(matcher.getTopicName()), matcher.getValue());
    } else if (matcher.isSubscriptionThroughput()) {
      counterStorage.incrementVolumeCounter(
          escapedTopicName(matcher.getTopicName()),
          escapeMetricsReplacementChar(matcher.getSubscriptionName()),
          matcher.getValue());
    }
  }

  private void reportCounter(CounterMatcher matcher) {
    if (matcher.getValue() == 0) {
      return;
    }

    if (matcher.isTopicPublished()) {
      counterStorage.setTopicPublishedCounter(
          escapedTopicName(matcher.getTopicName()), matcher.getValue());
    } else if (matcher.isSubscriptionDelivered()) {
      counterStorage.setSubscriptionDeliveredCounter(
          escapedTopicName(matcher.getTopicName()),
          escapeMetricsReplacementChar(matcher.getSubscriptionName()),
          matcher.getValue());
    } else if (matcher.isSubscriptionDiscarded()) {
      counterStorage.setSubscriptionDiscardedCounter(
          escapedTopicName(matcher.getTopicName()),
          escapeMetricsReplacementChar(matcher.getSubscriptionName()),
          matcher.getValue());
    }
  }

  private static TopicName escapedTopicName(TopicName topicName) {
    return new TopicName(
        escapeMetricsReplacementChar(topicName.getGroupName()), topicName.getName());
  }

  private static String escapeMetricsReplacementChar(String value) {
    return value.replaceAll(PathsCompiler.REPLACEMENT_CHAR, "\\.");
  }
}
