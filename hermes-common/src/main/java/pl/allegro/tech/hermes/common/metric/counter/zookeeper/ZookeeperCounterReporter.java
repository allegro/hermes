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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.metric.Metrics;
import pl.allegro.tech.hermes.common.metric.counter.CounterMatcher;
import pl.allegro.tech.hermes.common.metric.counter.CounterStorage;

import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import static pl.allegro.tech.hermes.api.TopicName.fromQualifiedName;

public class ZookeeperCounterReporter extends ScheduledReporter {

    private static final String ZOOKEEPER_REPORTER_NAME = "zookeeper-reporter";

    private static final TimeUnit RATE_UNIT = TimeUnit.SECONDS;

    private static final TimeUnit DURATION_UNIT = TimeUnit.MILLISECONDS;

    private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperCounterReporter.class);

    private final CounterStorage counterStorage;
    private final String graphitePrefix;

    public ZookeeperCounterReporter(MetricRegistry registry, CounterStorage counterStorage, ConfigFactory config) {
        super(
            registry,
            ZOOKEEPER_REPORTER_NAME,
            new CountersExceptOffsetsFilter(config.getStringProperty(Configs.GRAPHITE_PREFIX)),
            RATE_UNIT,
            DURATION_UNIT
        );
        this.counterStorage = counterStorage;
        this.graphitePrefix = config.getStringProperty(Configs.GRAPHITE_PREFIX);
    }

    @Override
    public void report(SortedMap<String, Gauge> gauges,
            SortedMap<String, Counter> counters,
            SortedMap<String, Histogram> histograms,
            SortedMap<String, Meter> meters,
            SortedMap<String, Timer> timers) {

        for (Map.Entry<String, Counter> entry : counters.entrySet()) {
            reportCounter(entry.getKey(), entry.getValue());
        }
    }

    private void reportCounter(String counterName, Counter counter) {
        if (counter.getCount() == 0) {
            return;
        }

        CounterMatcher matcher = new CounterMatcher(graphitePrefix, counterName);
        if (!matcher.matches()) {
            LOGGER.warn("Unknown counter {}", counterName);
            return;
        }

        long value = counter.getCount();
        if (matcher.isInflight()) {
            counterStorage.setInflightCounter(
                    matcher.getHostname(),
                    escapedTopicName(matcher.getTopicName()),
                    escapeMetricsReplacementChar(matcher.getSubscriptionName()),
                    value
            );
        } else if (matcher.isTopic()) {
            counterStorage.setTopicCounter(
                    escapedTopicName(matcher.getTopicName()),
                    matcher.getCounter(),
                    value
            );
        } else if (matcher.isSubscription()) {
            counterStorage.setSubscriptionCounter(
                    escapedTopicName(matcher.getTopicName()),
                    escapeMetricsReplacementChar(matcher.getSubscriptionName()),
                    matcher.getCounter(),
                    value
            );
        }
    }

    private static TopicName escapedTopicName(String qualifiedTopicName) {
        TopicName topicName = fromQualifiedName(qualifiedTopicName);
        return new TopicName(
                escapeMetricsReplacementChar(topicName.getGroupName()),
                escapeMetricsReplacementChar(topicName.getName())
        );
    }

    private static String escapeMetricsReplacementChar(String value) {
        return value.replaceAll(Metrics.REPLACEMENT_CHAR, "\\.");
    }

    private static final class CountersExceptOffsetsFilter implements MetricFilter {
        private final String offsetPrefix;

        private CountersExceptOffsetsFilter(String graphitePrefix) {
            offsetPrefix = graphitePrefix + "." + Metrics.OFFSET_PREFIX;
        }

        @Override
        public boolean matches(String name, Metric metric) {
            return metric instanceof Counter && !name.startsWith(offsetPrefix);
        }
    }
}
