package pl.allegro.tech.hermes.common.metric;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.util.HostnameResolver;

import java.util.Locale;

public final class Metrics {

    public static final String OFFSET_PREFIX = "consumer.offset";
    public static final String ESCAPED_HOSTNAME = escapeDots(HostnameResolver.detectHostname());
    public static final String REPLACEMENT_CHAR = "_";

    private Metrics() { }

    public static enum Timer {
        CONSUMER_LATENCY,
        CONSUMER_READ_LATENCY,
        CONSUMER_TRACKER_COMMIT_LATENCY,
        PRODUCER_BROKER_LATENCY,
        PRODUCER_PARSING_REQUEST,
        PRODUCER_LATENCY,
        PRODUCER_TRACKER_COMMIT_LATENCY,
        PRODUCER_VALIDATION_LATENCY;

        public String displayName(String graphitePrefix) {
            return Metrics.displayName(graphitePrefix, name());
        }
    }

    public static enum Meter {
        CONSUMER_SENDER_METER,
        CONSUMER_BROKER_METER,

        CONSUMER_METER,
        CONSUMER_FAILED_METER,
        CONSUMER_DISCARDED_METER,

        PRODUCER_METER,
        PRODUCER_FAILED_METER;

        public String displayName(String graphitePrefix) {
            return Metrics.displayName(graphitePrefix, name());
        }

        public String normalizedName() {
            return Metrics.normalizeName(cutSource(name()));
        }
    }

    public static enum Counter {
        CONSUMER_DELIVERED,
        CONSUMER_DISCARDED,
        CONSUMER_INFLIGHT,

        PRODUCER_PUBLISHED,
        PRODUCER_UNPUBLISHED;

        private final String source;
        private final String normalizedName;

        Counter() {
            source = Metrics.metricSource(name());
            normalizedName = Metrics.normalizeName(cutSource(name()));
        }

        @SuppressWarnings("PMD.UselessParentheses")
        public static Counter lookup(String source, String normalizedName) {
            return Counter.valueOf((source + "_" + normalizedName).toUpperCase(Locale.getDefault()));
        }

        public String displayName(String graphitePrefix) {
            return Metrics.displayName(graphitePrefix, metricSource(), cutSource(name()));
        }

        public String metricSource() {
            return source;
        }

        public String normalizedName() {
            return normalizedName;
        }
    }

    public static enum Gauge {
        PRODUCER_EVERYONE_CONFIRMS_BUFFER_TOTAL_BYTES,
        PRODUCER_EVERYONE_CONFIRMS_BUFFER_AVAILABLE_BYTES,
        PRODUCER_LEADER_CONFIRMS_BUFFER_TOTAL_BYTES,
        PRODUCER_LEADER_CONFIRMS_BUFFER_AVAILABLE_BYTES,
        PRODUCER_TRACKER_QUEUE_SIZE,
        PRODUCER_TRACKER_REMAINING_CAPACITY,
        CONSUMER_TRACKER_QUEUE_SIZE,
        CONSUMER_TRACKER_REMAINING_CAPACITY;

        public String displayName(String graphitePrefix) {
            return Metrics.displayName(graphitePrefix, name());
        }
    }

    public static String getOutputRatePath(String graphitePrefix) {
        return Metrics.displayName(graphitePrefix, "CONSUMER_OUTPUT_RATE");
    }

    public static String getConsumersThreadsPath(String graphitePrefix) {
        return String.format("%s.consumer.%s.threads", graphitePrefix, ESCAPED_HOSTNAME);
    }

    public static String getOffsetLagPath(Subscription subscription, int partition, String environmentName, String graphite) {
        return String.format("%s.%d.lag", getOffsetPath(subscription, environmentName, graphite), partition);
    }

    public static String getOffsetTimeLagPath(Subscription subscription, int partition, String environmentName, String graphite) {
        return String.format("%s.%d.timeLag", getOffsetPath(subscription, environmentName, graphite), partition);
    }

    public static String getPublisherStatusCodePath(int statusCode, String graphitePrefix) {
        return String.format("%s.producer.%s.http-status-codes.code%d", graphitePrefix, ESCAPED_HOSTNAME, statusCode);
    }

    public static String getPublisherStatusCodePath(int statusCode, String graphitePrefix, TopicName topicName) {
        return String.format("%s.producer.%s.http-status-codes.%s.%s.code%d",
            graphitePrefix,
            ESCAPED_HOSTNAME,
            escapeDots(topicName.getGroupName()),
            escapeDots(topicName.getName()),
            statusCode);
    }

    public static String escapeDots(String value) {
        return value.replaceAll("\\.", REPLACEMENT_CHAR);
    }

    private static String displayName(String graphitePrefix, String fullName) {
        return displayName(graphitePrefix, metricSource(fullName), cutSource(fullName));
    }

    private static String displayName(String graphitePrefix, String source, String name) {
        return Joiner.on(".").join(ImmutableList.of(graphitePrefix, source, ESCAPED_HOSTNAME, normalizeName(name)));
    }

    private static String cutSource(String name) {
        return name.substring(name.indexOf('_') + 1);
    }

    private static String normalizeName(String name) {
        return name.toLowerCase(Locale.getDefault()).replaceAll("_", "-");
    }

    private static String metricSource(String name) {
        return name.toLowerCase(Locale.getDefault()).substring(0, name.indexOf('_'));
    }

    private static String getOffsetPath(Subscription subscription, String environmentName, String graphitePrefix) {
        return MetricRegistry.name(
                graphitePrefix,
                OFFSET_PREFIX,
                environmentName,
                escapeDots(subscription.getTopicName().getGroupName()),
                escapeDots(subscription.getTopicName().getName()),
                escapeDots(subscription.getName())
        );
    }

}
