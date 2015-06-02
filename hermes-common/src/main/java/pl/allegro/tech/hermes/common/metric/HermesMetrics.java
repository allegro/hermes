package pl.allegro.tech.hermes.common.metric;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.yammer.metrics.core.HealthCheck;
import com.yammer.metrics.core.HealthCheckRegistry;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.metric.counter.CounterStorage;
import pl.allegro.tech.hermes.common.metric.counter.zookeeper.ZookeeperCounterReporter;
import pl.allegro.tech.hermes.common.util.HostnameResolver;

import javax.inject.Inject;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static pl.allegro.tech.hermes.common.metric.Metrics.Counter.CONSUMER_INFLIGHT;
import static pl.allegro.tech.hermes.common.metric.Metrics.Gauge.PRODUCER_EVERYONE_CONFIRMS_BUFFER_AVAILABLE_BYTES;
import static pl.allegro.tech.hermes.common.metric.Metrics.Gauge.PRODUCER_EVERYONE_CONFIRMS_BUFFER_TOTAL_BYTES;
import static pl.allegro.tech.hermes.common.metric.Metrics.Gauge.PRODUCER_LEADER_CONFIRMS_BUFFER_AVAILABLE_BYTES;
import static pl.allegro.tech.hermes.common.metric.Metrics.Gauge.PRODUCER_LEADER_CONFIRMS_BUFFER_TOTAL_BYTES;
import static pl.allegro.tech.hermes.common.metric.Metrics.escapeDots;

public class HermesMetrics {

    private final ConfigFactory configFactory;

    private final MetricRegistry metricRegistry;

    private final HealthCheckRegistry healthCheckRegistry;

    private final CounterStorage counterStorage;

    private final String graphitePrefix;

    private final String environmentName;

    @Inject
    public HermesMetrics(
            ConfigFactory configFactory,
            MetricRegistry metricRegistry,
            HealthCheckRegistry healthCheckRegistry,
            CounterStorage counterStorage) throws Exception {

        this.configFactory = configFactory;
        this.metricRegistry = metricRegistry;
        this.healthCheckRegistry = healthCheckRegistry;
        this.counterStorage = counterStorage;
        this.graphitePrefix = configFactory.getStringProperty(Configs.GRAPHITE_PREFIX);
        this.environmentName = configFactory.getStringProperty(Configs.ENVIRONMENT_NAME);

        prepareReporters();
    }

    private void prepareReporters() throws Exception {
        if (configFactory.getBooleanProperty(Configs.METRICS_GRAPHITE_REPORTER)) {
            GraphiteReporter
                .forRegistry(metricRegistry)
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
            new ZookeeperCounterReporter(metricRegistry, counterStorage, configFactory).start(
                configFactory.getIntProperty(Configs.REPORT_PERIOD),
                TimeUnit.SECONDS
            );
        }
    }

    public Timer timer(Metrics.Timer metric) {
        return metricRegistry.timer(MetricRegistry.name(metric.displayName(graphitePrefix)));
    }

    public Timer timer(Metrics.Timer metric, TopicName topicName) {
        return metricRegistry.timer(metricRegistryName(metric.displayName(graphitePrefix), topicName));
    }

    public Timer timer(Metrics.Timer metric, TopicName topicName, String name) {
        return metricRegistry.timer(metricRegistryName(metric.displayName(graphitePrefix), topicName, name));
    }

    public Meter meter(Metrics.Meter metric) {
        return metricRegistry.meter(MetricRegistry.name(metric.displayName(graphitePrefix)));
    }

    public Meter meter(Metrics.Meter metric, TopicName topicName, String name) {
        return metricRegistry.meter(metricRegistryName(metric.displayName(graphitePrefix), topicName, name));
    }

    public Meter meter(Metrics.Meter metric, TopicName topicName) {
        return metricRegistry.meter(metricRegistryName(metric.displayName(graphitePrefix), topicName));
    }

    public Meter httpStatusCodeMeter(int statusCode) {
        return metricRegistry.meter(Metrics.getPublisherStatusCodePath(statusCode, graphitePrefix));
    }

    public Meter httpStatusCodeMeter(int statusCode, TopicName topicName) {
        return metricRegistry.meter(Metrics.getPublisherStatusCodePath(statusCode, graphitePrefix, topicName));
    }

    public Counter counter(Metrics.Counter metric, TopicName topicName) {
        return metricRegistry.counter(metricRegistryName(metric.displayName(graphitePrefix), topicName));
    }

    public Counter counter(Metrics.Counter metric, TopicName topicName, String name) {
        return metricRegistry.counter(metricRegistryName(metric.displayName(graphitePrefix), topicName, name));
    }

    public Counter counterForOffsetLag(Subscription subscription, int partition) {
        return metricRegistry.counter(Metrics.getOffsetLagPath(subscription, partition, environmentName, graphitePrefix));
    }

    public Counter counterForOffsetCommitIdlePeriod(Subscription subscription, int partition) {
        return metricRegistry.counter(Metrics.getOffsetCommitIdlePeriodPath(graphitePrefix, subscription, partition));
    }

    public void removeCounterForOffsetCommitIdlePeriod(Subscription subscription, int partition) {
        metricRegistry.remove(Metrics.getOffsetCommitIdlePeriodPath(graphitePrefix, subscription, partition));
    }


    public Histogram histogramForOffsetTimeLag(Subscription subscription, int partition) {
        return metricRegistry.histogram(Metrics.getOffsetTimeLagPath(subscription, partition, environmentName, graphitePrefix));
    }

    public void registerConsumersThreadGauge(Gauge<Integer> gauge) {
        metricRegistry.register(Metrics.getConsumersThreadsPath(graphitePrefix), gauge);
    }

    public <T> void registerOutputRateGauge(TopicName topicName, String name, Gauge<T> gauge) {
        String normalizedMetricName = metricRegistryName(Metrics.getOutputRatePath(graphitePrefix), topicName, name);
        metricRegistry.register(normalizedMetricName, gauge);
    }

    public void unregisterOutputRateGauge(TopicName topicName, String name) {
        String normalizedMetricName = metricRegistryName(Metrics.getOutputRatePath(graphitePrefix), topicName, name);
        metricRegistry.remove(normalizedMetricName);
    }

    public LatencyTimer latencyTimer(Subscription subscription) {
        return new LatencyTimer(this, subscription.getTopicName(), subscription.getName());
    }

    public void incrementInflightCounter(Subscription subscription) {
        getInflightCounter(subscription).inc();
    }

    public void decrementInflightCounter(Subscription subscription) {
        getInflightCounter(subscription).dec();
    }

    public long getInflightCounterValue(Subscription subscription) {
        return getInflightCounter(subscription).getCount();
    }

    public void healthcheck(HealthCheck healthCheck) {
        healthCheckRegistry.register(healthCheck);
    }

    public Map<String, HealthCheck.Result> runHealthchecks() {
        return healthCheckRegistry.runHealthChecks();
    }

    public double getConsumerMeterOneMinuteRate(Subscription subscription) {
        return meter(Metrics.Meter.CONSUMER_METER, subscription.getTopicName(), subscription.getName()).getOneMinuteRate();
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

    private double getDoubleValue(Metrics.Gauge gauge) {
        return (double) metricRegistry.getGauges().get(gauge.displayName(graphitePrefix)).getValue();
    }

    private Counter getInflightCounter(Subscription subscription) {
        return counter(CONSUMER_INFLIGHT, subscription.getTopicName(), subscription.getName());
    }

    private String metricRegistryName(String metricDisplayName, TopicName topicName, String name) {
        return MetricRegistry.name(
            metricDisplayName, escapeDots(topicName.getGroupName()), escapeDots(topicName.getName()), escapeDots(name)
        );
    }

    private String metricRegistryName(String metricDisplayName, TopicName topicName) {
        return MetricRegistry.name(
            metricDisplayName, escapeDots(topicName.getGroupName()), escapeDots(topicName.getName())
        );
    }

    public int countActiveConsumers(Subscription subscription) {
        // This is an ad-hoc implementation, utilizing exising inflight nodes.
        return counterStorage.countInflightNodes(subscription.getTopicName(), subscription.getName());
    }

    public void removeMetrics(final Subscription subscription) {
        metricRegistry.removeMatching(new MetricFilter() {
            @Override
            public boolean matches(String name, Metric metric) {
                return name.contains(String.format(".%s.", subscription.getId()));
            }
        });
    }

    public void registerGauge(Metrics.Gauge name, Gauge<?> gauge) {
        metricRegistry.register(name.displayName(graphitePrefix), gauge);
    }

    public MetricRegistry getMetricRegistry() {
        return metricRegistry;
    }

    public String getThreadPoolName(String name) {
        return HostnameResolver.detectHostname() + ".executors." + name;
    }
}
