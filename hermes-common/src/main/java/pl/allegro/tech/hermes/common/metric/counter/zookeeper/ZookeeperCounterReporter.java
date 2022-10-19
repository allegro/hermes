package pl.allegro.tech.hermes.common.metric.counter.zookeeper;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Timer;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.Meters;
import pl.allegro.tech.hermes.common.metric.counter.CounterStorage;

import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import static pl.allegro.tech.hermes.api.TopicName.fromQualifiedName;

public class ZookeeperCounterReporter extends ScheduledReporter {

    private static final String ZOOKEEPER_REPORTER_NAME = "zookeeper-reporter";
    private static final TimeUnit RATE_UNIT = TimeUnit.SECONDS;
    private static final TimeUnit DURATION_UNIT = TimeUnit.MILLISECONDS;

    private final CounterStorage counterStorage;

    public ZookeeperCounterReporter(MetricRegistry registry,
                                    CounterStorage counterStorage,
                                    String graphitePrefix
    ) {
        super(
                registry,
                ZOOKEEPER_REPORTER_NAME,
                new ZookeeperMetricsFilter(graphitePrefix),
                RATE_UNIT,
                DURATION_UNIT
        );
        this.counterStorage = counterStorage;
    }

    @Override
    public void report(SortedMap<String, Gauge> gauges,
                       SortedMap<String, Counter> counters,
                       SortedMap<String, Histogram> histograms,
                       SortedMap<String, Meter> meters,
                       SortedMap<String, Timer> timers) {

        counters.forEach(this::reportCounter);

        meters
                .entrySet()
                .stream()
                .filter(meterEntry -> meterEntry.getKey().startsWith(Meters.THROUGHPUT_BYTES))
                .forEach(meterEntry -> reportVolumeCounter(meterEntry.getKey(), meterEntry.getValue().getCount()));
    }

    private void reportVolumeCounter(String metricName, long value) {
        CounterMatcher matcher = new CounterMatcher(metricName);
        if (matcher.isTopicThroughput()) {
            counterStorage.incrementVolumeCounter(
                    escapedTopicName(matcher.getTopicName()),
                    value
            );
        } else if (matcher.isSubscriptionThroughput()) {
            counterStorage.incrementVolumeCounter(
                    escapedTopicName(matcher.getTopicName()),
                    escapeMetricsReplacementChar(matcher.getSubscriptionName()),
                    value
            );
        }
    }

    private void reportCounter(String counterName, Counter counter) {
        if (counter.getCount() == 0) {
            return;
        }

        CounterMatcher matcher = new CounterMatcher(counterName);
        long value = counter.getCount();

        if (matcher.isTopicPublished()) {
            counterStorage.setTopicPublishedCounter(
                    escapedTopicName(matcher.getTopicName()),
                    value
            );
        } else if (matcher.isSubscriptionDelivered()) {
            counterStorage.setSubscriptionDeliveredCounter(
                    escapedTopicName(matcher.getTopicName()),
                    escapeMetricsReplacementChar(matcher.getSubscriptionName()),
                    value
            );
        } else if (matcher.isSubscriptionDiscarded()) {
            counterStorage.setSubscriptionDiscardedCounter(
                    escapedTopicName(matcher.getTopicName()),
                    escapeMetricsReplacementChar(matcher.getSubscriptionName()),
                    value
            );
        }
    }

    private static TopicName escapedTopicName(String qualifiedTopicName) {
        TopicName topicName = fromQualifiedName(qualifiedTopicName);
        return new TopicName(
                escapeMetricsReplacementChar(topicName.getGroupName()),
                topicName.getName()
        );
    }

    private static String escapeMetricsReplacementChar(String value) {
        return value.replaceAll(HermesMetrics.REPLACEMENT_CHAR, "\\.");
    }

    private static final class ZookeeperMetricsFilter implements MetricFilter {
        private final String offsetPrefix;

        private ZookeeperMetricsFilter(String graphitePrefix) {
            offsetPrefix = graphitePrefix + "." + "consumer.offset";
        }

        @Override
        public boolean matches(String name, Metric metric) {
            return (metric instanceof Counter && !name.startsWith(offsetPrefix))
                    || (metric instanceof Meter && name.startsWith(Meters.THROUGHPUT_BYTES + "."));
        }
    }
}
