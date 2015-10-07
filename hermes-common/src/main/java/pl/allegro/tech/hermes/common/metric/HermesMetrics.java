package pl.allegro.tech.hermes.common.metric;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.metric.counter.CounterStorage;
import pl.allegro.tech.hermes.common.metric.counter.zookeeper.ZookeeperCounterReporter;
import pl.allegro.tech.hermes.common.metric.timer.ConsumerLatencyTimer;
import pl.allegro.tech.hermes.common.util.HostnameResolver;
import pl.allegro.tech.hermes.metrics.PathContext;
import pl.allegro.tech.hermes.metrics.PathsCompiler;

import javax.inject.Inject;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import static pl.allegro.tech.hermes.common.metric.Gauges.PRODUCER_EVERYONE_CONFIRMS_BUFFER_AVAILABLE_BYTES;
import static pl.allegro.tech.hermes.common.metric.Gauges.PRODUCER_EVERYONE_CONFIRMS_BUFFER_TOTAL_BYTES;
import static pl.allegro.tech.hermes.common.metric.Gauges.PRODUCER_LEADER_CONFIRMS_BUFFER_AVAILABLE_BYTES;
import static pl.allegro.tech.hermes.common.metric.Gauges.PRODUCER_LEADER_CONFIRMS_BUFFER_TOTAL_BYTES;
import static pl.allegro.tech.hermes.metrics.PathContext.pathContext;

public class HermesMetrics {

    public static final String REPLACEMENT_CHAR = "_";

    private final ConfigFactory configFactory;
    private final MetricRegistry metricRegistry;
    private final CounterStorage counterStorage;
    private final PathsCompiler pathCompiler;
    private final HostnameResolver hostnameResolver;

    @Inject
    public HermesMetrics(
            ConfigFactory configFactory,
            MetricRegistry metricRegistry,
            CounterStorage counterStorage,
            PathsCompiler pathCompiler,
            HostnameResolver hostnameResolver) throws Exception {

        this.configFactory = configFactory;
        this.metricRegistry = metricRegistry;
        this.counterStorage = counterStorage;
        this.pathCompiler = pathCompiler;
        this.hostnameResolver = hostnameResolver;

        prepareReporters();
    }

    private void prepareReporters() throws Exception {
        if (configFactory.getBooleanProperty(Configs.METRICS_GRAPHITE_REPORTER)) {
            GraphiteReporter
                    .forRegistry(metricRegistry)
                    .prefixedWith(configFactory.getStringProperty(Configs.GRAPHITE_PREFIX))
                    .build(new Graphite(new InetSocketAddress(
                            configFactory.getStringProperty(Configs.GRAPHITE_HOST),
                            configFactory.getIntProperty(Configs.GRAPHITE_PORT)
                    )))
                    .start(configFactory.getIntProperty(Configs.REPORT_PERIOD), TimeUnit.SECONDS);
        }
        if (configFactory.getBooleanProperty(Configs.METRICS_CONSOLE_REPORTER)) {
            ConsoleReporter.forRegistry(metricRegistry).build().start(
                    configFactory.getIntProperty(Configs.REPORT_PERIOD), TimeUnit.SECONDS
            );
        }

        if (configFactory.getBooleanProperty(Configs.METRICS_ZOOKEEPER_REPORTER)) {
            new ZookeeperCounterReporter(metricRegistry, counterStorage, hostnameResolver, configFactory).start(
                    configFactory.getIntProperty(Configs.REPORT_PERIOD),
                    TimeUnit.SECONDS
            );
        }
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
        return metricRegistry.meter(pathCompiler.compile(Meters.PRODUCER_STATUS_CODES, pathContext().withHttpCode(statusCode).build()));
    }

    public Meter httpStatusCodeMeter(int statusCode, TopicName topicName) {
        return metricRegistry.meter(pathCompiler.compile(Meters.PRODUCER_TOPIC_STATUS_CODES,
                pathContext().withHttpCode(statusCode).withGroup(topicName.getGroupName()).withTopic(topicName.getName()).build()));
    }

    public Counter counter(String metric, TopicName topicName) {
        return metricRegistry.counter(metricRegistryName(metric, topicName));
    }

    public Counter counter(String metric, TopicName topicName, String name) {
        return metricRegistry.counter(metricRegistryName(metric, topicName, name));
    }

    public Counter counterForOffsetCommitIdlePeriod(Subscription subscription, int partition) {
        String path = pathForConsumerOffsetCommitIdle(subscription, partition);

        return metricRegistry.counter(path);
    }

    public void removeCounterForOffsetCommitIdlePeriod(Subscription subscription, int partition) {
        String path = pathForConsumerOffsetCommitIdle(subscription, partition);

        metricRegistry.remove(path);
    }

    private String pathForConsumerOffsetCommitIdle(Subscription subscription, int partition) {
        return pathCompiler.compile(Counters.CONSUMER_OFFSET_COMMIT_IDLE, pathContext()
                .withGroup(escapeDots(subscription.getTopicName().getGroupName()))
                .withTopic(escapeDots(subscription.getTopicName().getName()))
                .withSubscription(escapeDots(subscription.getName()))
                .withPartition(partition)
                .build());
    }

    public void registerConsumersThreadGauge(Gauge<Integer> gauge) {
        metricRegistry.register(metricRegistryName(Gauges.CONSUMER_THREADS), gauge);
    }

    public <T> void registerOutputRateGauge(TopicName topicName, String name, Gauge<T> gauge) {
        metricRegistry.register(metricRegistryName(Gauges.CONSUMER_OUTPUT_RATE, topicName, name), gauge);
    }

    public void unregisterOutputRateGauge(TopicName topicName, String name) {
        String normalizedMetricName = metricRegistryName(Gauges.CONSUMER_OUTPUT_RATE, topicName, name);
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

    public static void close(Timer.Context... timers) {
        for (Timer.Context timer : timers) {
            if (timer != null) {
                timer.close();
            }
        }
    }

    public double getBufferTotalBytes() {
        return getDoubleValue(PRODUCER_LEADER_CONFIRMS_BUFFER_TOTAL_BYTES)
                + getDoubleValue(PRODUCER_EVERYONE_CONFIRMS_BUFFER_TOTAL_BYTES);
    }

    public double getBufferAvailablesBytes() {
        return getDoubleValue(PRODUCER_LEADER_CONFIRMS_BUFFER_AVAILABLE_BYTES)
                + getDoubleValue(PRODUCER_EVERYONE_CONFIRMS_BUFFER_AVAILABLE_BYTES);
    }

    private double getDoubleValue(String gauge) {
        return (double) metricRegistry.getGauges().get(pathCompiler.compile(gauge)).getValue();
    }

    private Counter getInflightCounter(Subscription subscription) {
        return counter(Counters.CONSUMER_INFLIGHT, subscription.getTopicName(), subscription.getName());
    }

    public int countActiveConsumers(Subscription subscription) {
        // This is an ad-hoc implementation, utilizing exising inflight nodes.
        return counterStorage.countInflightNodes(subscription.getTopicName(), subscription.getName());
    }

    public void removeMetrics(final SubscriptionName subscription) {
        metricRegistry.removeMatching((name, metric) -> name.contains(String.format(".%s.", subscription.getId())));
    }

    public void registerGauge(String name, Gauge<?> gauge) {
        metricRegistry.register(pathCompiler.compile(name), gauge);
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

    public Timer executorDurationTimer(String executorName) {
        return metricRegistry.timer(pathCompiler.compile(Timers.CONSUMER_EXECUTOR_DURATION, pathContext().withExecutorName(executorName).build()));
    }

    public Timer executorWaitingTimer(String executorName) {
        return metricRegistry.timer(pathCompiler.compile(Timers.CONSUMER_EXECUTOR_WAITING, pathContext().withExecutorName(executorName).build()));
    }

    public Meter executorCompletedMeter(String executorName) {
        return metricRegistry.meter(pathCompiler.compile(Meters.CONSUMER_EXECUTOR_COMPLETED, pathContext().withExecutorName(executorName).build()));
    }

    public Meter executorSubmittedMeter(String executorName) {
        return metricRegistry.meter(pathCompiler.compile(Meters.CONSUMER_EXECUTOR_SUBMITTED, pathContext().withExecutorName(executorName).build()));
    }

    public Counter executorRunningCounter(String executorName) {
        return metricRegistry.counter(pathCompiler.compile(Counters.CONSUMER_EXECUTOR_RUNNING, pathContext().withExecutorName(executorName).build()));
    }

    public Counter scheduledExecutorOverrun(String executorName) {
        return metricRegistry.counter(pathCompiler.compile(Counters.CONSUMER_SCHEDULED_EXECUTOR_OVERRUN, pathContext().withExecutorName(executorName).build()));
    }

    public Histogram messageContentSizeHistogram(TopicName topic) {
        return metricRegistry.histogram(pathCompiler.compile(Histograms.PRODUCER_MESSAGE_SIZE, pathContext()
                .withGroup(escapeDots(topic.getGroupName()))
                .withTopic(escapeDots(topic.getName()))
                .build()));
    }

    public Histogram inflightTimeHistogram(Subscription subscription) {
        return metricRegistry.histogram(pathCompiler.compile(Histograms.CONSUMER_INFLIGHT_TIME, pathContext()
                .withGroup(escapeDots(subscription.getTopicName().getGroupName()))
                .withTopic(escapeDots(subscription.getTopicName().getName()))
                .withSubscription(escapeDots(subscription.getName()))
                .build()));
    }

    public void reportContentSize(int size, TopicName topicName) {
        messageContentSizeHistogram(topicName).update(size);
        metricRegistry.histogram(pathCompiler.compile(Histograms.PRODUCER_GLOBAL_MESSAGE_SIZE)).update(size);
    }

    public void registerConsumerHttpAnswer(Subscription subscription, int statusCode) {
        PathContext pathContext = pathContext()
                .withGroup(escapeDots(subscription.getTopicName().getGroupName()))
                .withTopic(escapeDots(subscription.getTopicName().getName()))
                .withSubscription(escapeDots(subscription.getName()))
                .withHttpCode(statusCode)
                .withHttpCodeFamily(httpStatusFamily(statusCode))
                .build();
        metricRegistry.meter(pathCompiler.compile(Meters.CONSUMER_ERRORS_HTTP_BY_FAMILY, pathContext)).mark();
        metricRegistry.meter(pathCompiler.compile(Meters.CONSUMER_ERRORS_HTTP_BY_CODE, pathContext)).mark();
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
        return metricRegistry.meter(pathCompiler.compile(Meters.CONSUMER_ERRORS_TIMEOUTS, pathContext));
    }

    public Meter consumerErrorsOtherMeter(Subscription subscription) {
        PathContext pathContext = pathContext()
                .withGroup(escapeDots(subscription.getTopicName().getGroupName()))
                .withTopic(escapeDots(subscription.getTopicName().getName()))
                .withSubscription(escapeDots(subscription.getName()))
                .build();
        return metricRegistry.meter(pathCompiler.compile(Meters.CONSUMER_ERRORS_OTHER, pathContext));
    }
}

