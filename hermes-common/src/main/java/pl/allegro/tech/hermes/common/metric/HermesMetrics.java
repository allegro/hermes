package pl.allegro.tech.hermes.common.metric;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.metric.timer.ConsumerLatencyTimer;
import pl.allegro.tech.hermes.common.schema.SchemaRepositoryType;
import pl.allegro.tech.hermes.metrics.PathContext;
import pl.allegro.tech.hermes.metrics.PathsCompiler;

import javax.inject.Inject;

import static pl.allegro.tech.hermes.common.metric.Gauges.EVERYONE_CONFIRMS_BUFFER_AVAILABLE_BYTES;
import static pl.allegro.tech.hermes.common.metric.Gauges.EVERYONE_CONFIRMS_BUFFER_TOTAL_BYTES;
import static pl.allegro.tech.hermes.common.metric.Gauges.LEADER_CONFIRMS_BUFFER_AVAILABLE_BYTES;
import static pl.allegro.tech.hermes.common.metric.Gauges.LEADER_CONFIRMS_BUFFER_TOTAL_BYTES;
import static pl.allegro.tech.hermes.common.metric.Timers.SUBSCRIPTION_LATENCY;
import static pl.allegro.tech.hermes.metrics.PathContext.pathContext;

public class HermesMetrics {

    public static final String REPLACEMENT_CHAR = "_";

    private final MetricRegistry metricRegistry;
    private final PathsCompiler pathCompiler;

    @Inject
    public HermesMetrics(
            MetricRegistry metricRegistry,
            PathsCompiler pathCompiler) {
        this.metricRegistry = metricRegistry;
        this.pathCompiler = pathCompiler;
    }

    public static String escapeDots(String value) {
        return value.replaceAll("\\.", REPLACEMENT_CHAR);
    }

    public Timer timer(String metric) {
        return metricRegistry.timer(metricRegistryName(metric));
    }

    public Timer timer(String metric, TopicName topicName) {
        return metricRegistry.timer(metricRegistryName(metric, topicName));
    }

    public Timer timer(String metric, TopicName topicName, String name) {
        return metricRegistry.timer(metricRegistryName(metric, topicName, name));
    }

    public Meter meter(String metric) {
        return metricRegistry.meter(metricRegistryName(metric));
    }

    public Meter meter(String metric, TopicName topicName, String name) {
        return metricRegistry.meter(metricRegistryName(metric, topicName, name));
    }

    public Meter meter(String metric, TopicName topicName) {
        return metricRegistry.meter(metricRegistryName(metric, topicName));
    }

    public Meter httpStatusCodeMeter(int statusCode) {
        return metricRegistry.meter(pathCompiler.compile(Meters.STATUS_CODES, pathContext().withHttpCode(statusCode).build()));
    }

    public Meter httpStatusCodeMeter(int statusCode, TopicName topicName) {
        return metricRegistry.meter(pathCompiler.compile(Meters.TOPIC_STATUS_CODES,
                pathContext().withHttpCode(statusCode).withGroup(topicName.getGroupName()).withTopic(topicName.getName()).build()));
    }

    public Counter counter(String metric) {
        return metricRegistry.counter(metricRegistryName(metric));
    }

    public Counter counter(String metric, TopicName topicName) {
        return metricRegistry.counter(metricRegistryName(metric, topicName));
    }

    public Counter counter(String metric, TopicName topicName, String name) {
        return metricRegistry.counter(metricRegistryName(metric, topicName, name));
    }

    public void registerProducerInflightRequest(Gauge<Integer> gauge) {
        metricRegistry.register(metricRegistryName(Gauges.INFLIGHT_REQUESTS), gauge);
    }

    public void registerConsumersThreadGauge(Gauge<Integer> gauge) {
        metricRegistry.register(metricRegistryName(Gauges.THREADS), gauge);
    }

    public void registerMessageRepositorySizeGauge(Gauge<Integer> gauge) {
        metricRegistry.register(metricRegistryName(Gauges.BACKUP_STORAGE_SIZE), gauge);
    }

    public <T> void registerOutputRateGauge(TopicName topicName, String name, Gauge<T> gauge) {
        metricRegistry.register(metricRegistryName(Gauges.OUTPUT_RATE, topicName, name), gauge);
    }

    public void unregisterOutputRateGauge(TopicName topicName, String name) {
        String normalizedMetricName = metricRegistryName(Gauges.OUTPUT_RATE, topicName, name);
        metricRegistry.remove(normalizedMetricName);
    }

    public ConsumerLatencyTimer latencyTimer(Subscription subscription) {
        return new ConsumerLatencyTimer(this, subscription.getTopicName(), subscription.getName());
    }

    public void incrementInflightCounter(Subscription subscription) {
        getInflightCounter(subscription).inc();
    }

    public void decrementInflightCounter(Subscription subscription) {
        getInflightCounter(subscription).dec();
    }

    public void decrementInflightCounter(Subscription subscription, int size) {
        getInflightCounter(subscription).dec(size);
    }

    public static void close(Timer.Context... timers) {
        for (Timer.Context timer : timers) {
            if (timer != null) {
                timer.close();
            }
        }
    }

    public double getBufferTotalBytes() {
        return getDoubleValue(LEADER_CONFIRMS_BUFFER_TOTAL_BYTES)
                + getDoubleValue(EVERYONE_CONFIRMS_BUFFER_TOTAL_BYTES);
    }

    public double getBufferAvailablesBytes() {
        return getDoubleValue(LEADER_CONFIRMS_BUFFER_AVAILABLE_BYTES)
                + getDoubleValue(EVERYONE_CONFIRMS_BUFFER_AVAILABLE_BYTES);
    }

    private double getDoubleValue(String gauge) {
        return (double) metricRegistry.getGauges().get(pathCompiler.compile(gauge)).getValue();
    }

    private Counter getInflightCounter(Subscription subscription) {
        return counter(Counters.INFLIGHT, subscription.getTopicName(), subscription.getName());
    }

    public void registerGauge(String name, Gauge<?> gauge) {
        String path = pathCompiler.compile(name);
        if (!metricRegistry.getGauges().containsKey(name)) {
            metricRegistry.register(path, gauge);
        }
    }

    private String metricRegistryName(String metricDisplayName, TopicName topicName, String subscription) {
        PathContext pathContext = PathContext.pathContext()
                .withGroup(escapeDots(topicName.getGroupName()))
                .withTopic(escapeDots(topicName.getName()))
                .withSubscription(escapeDots(subscription))
                .build();

        return pathCompiler.compile(metricDisplayName, pathContext);
    }

    private String metricRegistryName(String metricDisplayName, TopicName topicName) {
        PathContext pathContext = PathContext.pathContext()
                .withGroup(escapeDots(topicName.getGroupName()))
                .withTopic(escapeDots(topicName.getName())).build();

        return pathCompiler.compile(metricDisplayName, pathContext);
    }

    private String metricRegistryName(String metricDisplayName) {
        return pathCompiler.compile(metricDisplayName);
    }

    public Timer schemaTimer(String schemaMetric, SchemaRepositoryType schemaRepoType) {
        return metricRegistry.timer(pathCompiler.compile(schemaMetric, pathContext().withSchemaRepoType(schemaRepoType.toString()).build()));
    }

    public Timer executorDurationTimer(String executorName) {
        return metricRegistry.timer(pathCompiler.compile(Timers.EXECUTOR_DURATION, pathContext().withExecutorName(executorName).build()));
    }

    public Timer executorWaitingTimer(String executorName) {
        return metricRegistry.timer(pathCompiler.compile(Timers.EXECUTOR_WAITING, pathContext().withExecutorName(executorName).build()));
    }

    public Meter executorCompletedMeter(String executorName) {
        return metricRegistry.meter(pathCompiler.compile(Meters.EXECUTOR_COMPLETED, pathContext().withExecutorName(executorName).build()));
    }

    public Meter executorSubmittedMeter(String executorName) {
        return metricRegistry.meter(pathCompiler.compile(Meters.EXECUTOR_SUBMITTED, pathContext().withExecutorName(executorName).build()));
    }

    public Counter executorRunningCounter(String executorName) {
        return metricRegistry.counter(pathCompiler.compile(Counters.EXECUTOR_RUNNING, pathContext().withExecutorName(executorName).build()));
    }

    public Counter scheduledExecutorOverrun(String executorName) {
        return metricRegistry.counter(pathCompiler.compile(Counters.SCHEDULED_EXECUTOR_OVERRUN, pathContext().withExecutorName(executorName).build()));
    }

    public Histogram messageContentSizeHistogram() {
        return metricRegistry.histogram(pathCompiler.compile(Histograms.GLOBAL_MESSAGE_SIZE));
    }

    public Histogram messageContentSizeHistogram(TopicName topic) {
        return metricRegistry.histogram(pathCompiler.compile(Histograms.MESSAGE_SIZE, pathContext()
                .withGroup(escapeDots(topic.getGroupName()))
                .withTopic(escapeDots(topic.getName()))
                .build()));
    }

    public Histogram inflightTimeHistogram(Subscription subscription) {
        return metricRegistry.histogram(pathCompiler.compile(Histograms.INFLIGHT_TIME, pathContext()
                .withGroup(escapeDots(subscription.getTopicName().getGroupName()))
                .withTopic(escapeDots(subscription.getTopicName().getName()))
                .withSubscription(escapeDots(subscription.getName()))
                .build()));
    }

    public void registerConsumerHttpAnswer(Subscription subscription, int statusCode) {
        PathContext pathContext = pathContext()
                .withGroup(escapeDots(subscription.getTopicName().getGroupName()))
                .withTopic(escapeDots(subscription.getTopicName().getName()))
                .withSubscription(escapeDots(subscription.getName()))
                .withHttpCode(statusCode)
                .withHttpCodeFamily(httpStatusFamily(statusCode))
                .build();
        metricRegistry.meter(pathCompiler.compile(Meters.ERRORS_HTTP_BY_FAMILY, pathContext)).mark();
        metricRegistry.meter(pathCompiler.compile(Meters.ERRORS_HTTP_BY_CODE, pathContext)).mark();
    }

    private String httpStatusFamily(int statusCode) {
        return String.format("%dxx", statusCode / 100);
    }

    public Meter consumerErrorsTimeoutMeter(Subscription subscription) {
        PathContext pathContext = pathContext()
                .withGroup(escapeDots(subscription.getTopicName().getGroupName()))
                .withTopic(escapeDots(subscription.getTopicName().getName()))
                .withSubscription(escapeDots(subscription.getName()))
                .build();
        return metricRegistry.meter(pathCompiler.compile(Meters.ERRORS_TIMEOUTS, pathContext));
    }

    public Meter consumerErrorsOtherMeter(Subscription subscription) {
        PathContext pathContext = pathContext()
                .withGroup(escapeDots(subscription.getTopicName().getGroupName()))
                .withTopic(escapeDots(subscription.getTopicName().getName()))
                .withSubscription(escapeDots(subscription.getName()))
                .build();
        return metricRegistry.meter(pathCompiler.compile(Meters.ERRORS_OTHER, pathContext));
    }

    public Timer consumersWorkloadRebalanceDurationTimer(String kafkaCluster) {
        PathContext pathContext = pathContext().withKafkaCluster(kafkaCluster).build();
        return metricRegistry.timer(pathCompiler.compile(Timers.CONSUMER_WORKLOAD_REBALANCE_DURATION, pathContext));
    }

    public Timer subscriptionLatencyTimer(Subscription subscription) {
        return timer(SUBSCRIPTION_LATENCY, subscription.getTopicName(), subscription.getName());
    }

    public Timer oAuthProviderLatencyTimer(String oAuthProviderName) {
        PathContext pathContext = pathContext()
                .withOAuthProvider(escapeDots(oAuthProviderName))
                .build();
        return metricRegistry.timer(pathCompiler.compile(Timers.OAUTH_PROVIDER_TOKEN_REQUEST_LATENCY, pathContext));
    }

    public Meter oAuthSubscriptionTokenRequestMeter(Subscription subscription, String oAuthProviderName) {
        PathContext pathContext = pathContext()
                .withGroup(escapeDots(subscription.getTopicName().getGroupName()))
                .withTopic(escapeDots(subscription.getTopicName().getName()))
                .withSubscription(escapeDots(subscription.getName()))
                .withOAuthProvider(escapeDots(oAuthProviderName))
                .build();
        return metricRegistry.meter(pathCompiler.compile(Meters.OAUTH_SUBSCRIPTION_TOKEN_REQUEST, pathContext));
    }

    public Counter rateHistoryFailuresCounter(Subscription subscription) {
        return metricRegistry.counter(metricRegistryName(
                Counters.MAXRATE_RATE_HISTORY_FAILURES, subscription.getTopicName(), subscription.getName()));
    }

    public Counter maxRateFetchFailuresCounter(Subscription subscription) {
        return metricRegistry.counter(metricRegistryName(
                Counters.MAXRATE_FETCH_FAILURES, subscription.getTopicName(), subscription.getName()));
    }

    public void registerMaxRateGauge(Subscription subscription, Gauge<Double> gauge) {
        metricRegistry.register(metricRegistryName(
                Gauges.MAX_RATE_VALUE, subscription.getTopicName(), subscription.getName()), gauge);
    }

    public void unregisterMaxRateGauge(Subscription subscription) {
        metricRegistry.remove(metricRegistryName(
                Gauges.MAX_RATE_VALUE, subscription.getTopicName(), subscription.getName()));
    }

    public void registerRateGauge(Subscription subscription, Gauge<Double> gauge) {
        metricRegistry.register(metricRegistryName(
                Gauges.MAX_RATE_ACTUAL_RATE_VALUE, subscription.getTopicName(), subscription.getName()), gauge);
    }

    public void unregisterRateGauge(Subscription subscription) {
        metricRegistry.remove(metricRegistryName(
                Gauges.MAX_RATE_ACTUAL_RATE_VALUE, subscription.getTopicName(), subscription.getName()));
    }
}

