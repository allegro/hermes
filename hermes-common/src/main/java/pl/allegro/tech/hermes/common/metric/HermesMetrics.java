package pl.allegro.tech.hermes.common.metric;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.metrics.PathContext;
import pl.allegro.tech.hermes.metrics.PathsCompiler;

import static pl.allegro.tech.hermes.common.metric.Gauges.ACK_ALL_BUFFER_AVAILABLE_BYTES;
import static pl.allegro.tech.hermes.common.metric.Gauges.ACK_ALL_BUFFER_TOTAL_BYTES;
import static pl.allegro.tech.hermes.common.metric.Gauges.ACK_LEADER_BUFFER_AVAILABLE_BYTES;
import static pl.allegro.tech.hermes.common.metric.Gauges.ACK_LEADER_BUFFER_TOTAL_BYTES;
import static pl.allegro.tech.hermes.common.metric.Histograms.INFLIGHT_TIME;
import static pl.allegro.tech.hermes.common.metric.Meters.ERRORS_HTTP_BY_CODE;
import static pl.allegro.tech.hermes.common.metric.Meters.ERRORS_HTTP_BY_FAMILY;
import static pl.allegro.tech.hermes.common.metric.Meters.ERRORS_OTHER;
import static pl.allegro.tech.hermes.common.metric.Meters.ERRORS_TIMEOUTS;
import static pl.allegro.tech.hermes.common.metric.Meters.SUBSCRIPTION_STATUS;
import static pl.allegro.tech.hermes.metrics.PathContext.pathContext;

public class HermesMetrics {

    public static final String REPLACEMENT_CHAR = "_";

    private final MetricRegistry metricRegistry;
    private final PathsCompiler pathCompiler;

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

    public Histogram histogram(String metric) {
        return metricRegistry.histogram(metricRegistryName(metric));
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

    public void registerConsumerSenderRequestQueueSize(Gauge<Integer> gauge) {
        metricRegistry.register(metricRegistryName(Gauges.CONSUMER_SENDER_REQUEST_QUEUE_SIZE), gauge);
    }

    public void registerConsumerSenderHttp1SerialClientRequestQueueSize(Gauge<Integer> gauge) {
        metricRegistry.register(metricRegistryName(Gauges.CONSUMER_SENDER_HTTP_1_SERIAL_CLIENT_REQUEST_QUEUE_SIZE), gauge);
    }

    public void registerConsumerSenderHttp1BatchClientRequestQueueSize(Gauge<Integer> gauge) {
        metricRegistry.register(metricRegistryName(Gauges.CONSUMER_SENDER_HTTP_1_BATCH_CLIENT_REQUEST_QUEUE_SIZE), gauge);
    }

    public void registerConsumerSenderHttp2RequestQueueSize(Gauge<Integer> gauge) {
        metricRegistry.register(metricRegistryName(Gauges.CONSUMER_SENDER_HTTP_2_SERIAL_CLIENT_REQUEST_QUEUE_SIZE), gauge);
    }

    public void registerThreadPoolActiveThreads(String executorName, Gauge<Integer> gauge) {
        registerExecutorGauge(Gauges.EXECUTOR_ACTIVE_THREADS, executorName, gauge);
    }

    public void registerThreadPoolCapacity(String executorName, Gauge<Integer> gauge) {
        registerExecutorGauge(Gauges.EXECUTOR_CAPACITY, executorName, gauge);
    }

    public void registerThreadPoolUtilization(String executorName, Gauge<Double> gauge) {
        registerExecutorGauge(Gauges.UTILIZATION, executorName, gauge);
    }

    public void registerThreadPoolTaskQueueCapacity(String executorName, Gauge<Integer> gauge) {
        registerExecutorGauge(Gauges.TASK_QUEUE_CAPACITY, executorName, gauge);
    }

    public void registerThreadPoolTaskQueued(String executorName, Gauge<Integer> gauge) {
        registerExecutorGauge(Gauges.TASK_QUEUED, executorName, gauge);
    }

    public void registerThreadPoolTaskQueueUtilization(String executorName, Gauge<Double> gauge) {
        registerExecutorGauge(Gauges.TASKS_QUEUE_UTILIZATION, executorName, gauge);
    }

    public void incrementThreadPoolTaskRejectedCount(String executorName) {
        executorCounter(Gauges.TASKS_REJECTED_COUNT, executorName).inc();
    }

    public void incrementInflightCounter(SubscriptionName subscription) {
        getInflightCounter(subscription).inc();
    }

    public void decrementInflightCounter(SubscriptionName subscription) {
        getInflightCounter(subscription).dec();
    }

    public void decrementInflightCounter(SubscriptionName subscription, int size) {
        getInflightCounter(subscription).dec(size);
    }

    public void unregisterInflightCounter(SubscriptionName subscription) {
        unregister(Counters.INFLIGHT, subscription);
    }

    public static void close(Timer.Context... timers) {
        for (Timer.Context timer : timers) {
            if (timer != null) {
                timer.close();
            }
        }
    }

    public double getBufferTotalBytes() {
        return getDoubleValue(ACK_LEADER_BUFFER_TOTAL_BYTES)
                + getDoubleValue(ACK_ALL_BUFFER_TOTAL_BYTES);
    }

    public double getBufferAvailablesBytes() {
        return getDoubleValue(ACK_LEADER_BUFFER_AVAILABLE_BYTES)
                + getDoubleValue(ACK_ALL_BUFFER_AVAILABLE_BYTES);
    }

    private double getDoubleValue(String gauge) {
        return (double) metricRegistry.getGauges().get(pathCompiler.compile(gauge)).getValue();
    }

    private Counter getInflightCounter(SubscriptionName subscription) {
        return counter(Counters.INFLIGHT, subscription.getTopicName(), subscription.getName());
    }

    public void registerGauge(String name, Gauge<?> gauge) {
        String path = pathCompiler.compile(name);
        if (!metricRegistry.getGauges().containsKey(name)) {
            metricRegistry.register(path, gauge);
        }
    }

    public void registerGauge(String name, SubscriptionName subscription, Gauge<?> gauge) {
        if (!metricRegistry.getGauges().containsKey(name)) {
            metricRegistry.register(metricRegistryName(name, subscription.getTopicName(), subscription.getName()), gauge);
        }
    }

    public void unregister(String metric, SubscriptionName subscription) {
        metricRegistry.remove(metricRegistryName(metric, subscription.getTopicName(), subscription.getName()));
    }

    public void unregister(String name) {
        String path = pathCompiler.compile(name);
        metricRegistry.remove(path);
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

    public Timer schemaTimer(String schemaMetric) {
        return metricRegistry.timer(pathCompiler.compile(schemaMetric, pathContext().withSchemaRepoType("schema-registry").build()));
    }

    private <T> Gauge<T> registerExecutorGauge(String path, String executorName, Gauge<T> gauge) {
        return metricRegistry.register(pathCompiler.compile(path, pathContext().withExecutorName(executorName).build()), gauge);
    }

    private <T> Counter executorCounter(String path, String executorName) {
        return metricRegistry.counter(pathCompiler.compile(path, pathContext().withExecutorName(executorName).build()));
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

    public Histogram inflightTimeHistogram(SubscriptionName subscription) {
        return metricRegistry.histogram(metricRegistryName(INFLIGHT_TIME, subscription.getTopicName(), subscription.getName()));
    }

    public void unregisterInflightTimeHistogram(SubscriptionName subscription) {
        unregister(INFLIGHT_TIME, subscription);
    }

    public void registerConsumerHttpAnswer(SubscriptionName subscription, int statusCode) {
        PathContext pathContext = pathContext()
                .withGroup(escapeDots(subscription.getTopicName().getGroupName()))
                .withTopic(escapeDots(subscription.getTopicName().getName()))
                .withSubscription(escapeDots(subscription.getName()))
                .withHttpCode(statusCode)
                .withHttpCodeFamily(httpStatusFamily(statusCode))
                .build();
        metricRegistry.meter(pathCompiler.compile(ERRORS_HTTP_BY_FAMILY, pathContext)).mark();
        metricRegistry.meter(pathCompiler.compile(ERRORS_HTTP_BY_CODE, pathContext)).mark();
    }

    public void unregisterStatusMeters(SubscriptionName subscription) {
        String prefix = metricRegistryName(SUBSCRIPTION_STATUS, subscription.getTopicName(), subscription.getName());
        metricRegistry.removeMatching(MetricFilter.startsWith(prefix));
    }

    private String httpStatusFamily(int statusCode) {
        return String.format("%dxx", statusCode / 100);
    }

    public Meter consumerErrorsTimeoutMeter(SubscriptionName subscription) {
        return metricRegistry.meter(metricRegistryName(ERRORS_TIMEOUTS, subscription.getTopicName(), subscription.getName()));
    }

    public void unregisterConsumerErrorsTimeoutMeter(SubscriptionName subscription) {
        unregister(ERRORS_TIMEOUTS, subscription);
    }

    public Meter consumerErrorsOtherMeter(SubscriptionName subscription) {
        return metricRegistry.meter(metricRegistryName(ERRORS_OTHER, subscription.getTopicName(), subscription.getName()));
    }

    public void unregisterConsumerErrorsOtherMeter(SubscriptionName subscription) {
        unregister(ERRORS_OTHER, subscription);
    }

    public Timer consumersWorkloadRebalanceDurationTimer(String kafkaCluster) {
        PathContext pathContext = pathContext().withKafkaCluster(kafkaCluster).build();
        return metricRegistry.timer(pathCompiler.compile(Timers.CONSUMER_WORKLOAD_REBALANCE_DURATION, pathContext));
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

    public void registerRunningConsumerProcessesCountGauge(Gauge<Integer> gauge) {
        metricRegistry.register(metricRegistryName(Gauges.RUNNING_CONSUMER_PROCESSES_COUNT), gauge);
    }

    public void registerDyingConsumerProcessesCountGauge(Gauge<Integer> gauge) {
        metricRegistry.register(metricRegistryName(Gauges.DYING_CONSUMER_PROCESSES_COUNT), gauge);
    }
}

