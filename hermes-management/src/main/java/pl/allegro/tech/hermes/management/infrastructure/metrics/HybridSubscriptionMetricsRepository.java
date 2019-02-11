package pl.allegro.tech.hermes.management.infrastructure.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.SubscriptionMetrics;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.counter.DistributedEphemeralCounter;
import pl.allegro.tech.hermes.infrastructure.zookeeper.counter.SharedCounter;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionLagSource;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionMetricsRepository;
import pl.allegro.tech.hermes.management.infrastructure.graphite.GraphiteClient;
import pl.allegro.tech.hermes.management.infrastructure.graphite.GraphiteMetrics;
import pl.allegro.tech.hermes.management.stub.MetricsPaths;

import java.util.function.Supplier;

import static org.apache.commons.lang.exception.ExceptionUtils.getRootCauseMessage;
import static pl.allegro.tech.hermes.common.metric.HermesMetrics.escapeDots;

@Component
public class HybridSubscriptionMetricsRepository implements SubscriptionMetricsRepository {

    private static final Logger logger = LoggerFactory.getLogger(HybridSubscriptionMetricsRepository.class);

    private static final String SUBSCRIPTION_PATH = "%s.%s.%s";

    private static final String SUBSCRIPTION_RATE_PATTERN = "sumSeries(%s.consumer.*.meter.%s.m1_rate)";
    private static final String SUBSCRIPTION_THROUGHPUT_PATTERN = "sumSeries(%s.consumer.*.throughput.%s.m1_rate)";
    private static final String SUBSCRIPTION_HTTP_STATUSES_PATTERN = "sumSeries(%s.consumer.*.status.%s.%s.m1_rate)";
    private static final String SUBSCRIPTION_ERROR_TIMEOUT_PATTERN = "sumSeries(%s.consumer.*.status.%s.errors.timeout.m1_rate)";
    private static final String SUBSCRIPTION_ERROR_OTHER_PATTERN = "sumSeries(%s.consumer.*.status.%s.errors.other.m1_rate)";
    private static final String SUBSCRIPTION_BATCH_RATE_PATTERN = "sumSeries(%s.consumer.*.meter.%s.batch.m1_rate)";

    private final GraphiteClient graphiteClient;

    private final MetricsPaths metricsPaths;

    private final SharedCounter sharedCounter;

    private final DistributedEphemeralCounter distributedCounter;

    private final ZookeeperPaths zookeeperPaths;

    private final SubscriptionLagSource lagSource;

    @Autowired
    public HybridSubscriptionMetricsRepository(GraphiteClient graphiteClient, MetricsPaths metricsPaths,
                                               SharedCounter sharedCounter, DistributedEphemeralCounter distributedCounter,
                                               ZookeeperPaths zookeeperPaths, SubscriptionLagSource lagSource) {
        this.graphiteClient = graphiteClient;
        this.metricsPaths = metricsPaths;
        this.sharedCounter = sharedCounter;
        this.distributedCounter = distributedCounter;
        this.zookeeperPaths = zookeeperPaths;
        this.lagSource = lagSource;
    }

    @Override
    public SubscriptionMetrics loadMetrics(TopicName topicName, String subscriptionName) {
        SubscriptionName name = new SubscriptionName(subscriptionName, topicName);

        String rateMetric = metricPath(name);
        String timeouts = metricPathTimeouts(name);
        String throughput = metricPathThroughput(name);
        String otherErrors = metricPathOtherErrors(name);
        String codes2xxPath = metricPathHttpStatuses(name, "2xx");
        String codes4xxPath = metricPathHttpStatuses(name, "4xx");
        String codes5xxPath = metricPathHttpStatuses(name, "5xx");
        String batchPath = metricPathBatchRate(name);

        GraphiteMetrics graphiteMetrics = graphiteClient.readMetrics(codes2xxPath, codes4xxPath, codes5xxPath,
                rateMetric, timeouts, otherErrors, batchPath);
        ZookeeperMetrics zookeeperMetrics = readZookeeperMetrics(name);

        return SubscriptionMetrics.Builder.subscriptionMetrics()
                .withRate(graphiteMetrics.metricValue(rateMetric))
                .withDelivered(zookeeperMetrics.delivered)
                .withDiscarded(zookeeperMetrics.discarded)
                .withInflight(zookeeperMetrics.inflight)
                .withCodes2xx(graphiteMetrics.metricValue(codes2xxPath))
                .withCodes4xx(graphiteMetrics.metricValue(codes4xxPath))
                .withCodes5xx(graphiteMetrics.metricValue(codes5xxPath))
                .withTimeouts(graphiteMetrics.metricValue(timeouts))
                .withOtherErrors(graphiteMetrics.metricValue(otherErrors))
                .withLag(lagSource.getLag(topicName, subscriptionName))
                .withThroughput(graphiteMetrics.metricValue(throughput))
                .withBatchRate(graphiteMetrics.metricValue(batchPath))
                .build();
    }

    private ZookeeperMetrics readZookeeperMetrics(SubscriptionName name) {
        return new ZookeeperMetrics(
                readZookeeperMetric(() -> sharedCounter.getValue(zookeeperPaths.subscriptionMetricPath(name, "delivered")), name),
                readZookeeperMetric(() -> sharedCounter.getValue(zookeeperPaths.subscriptionMetricPath(name, "discarded")), name),
                readZookeeperMetric(() -> distributedCounter.getValue(
                        zookeeperPaths.consumersPath(),
                        zookeeperPaths.subscriptionMetricPathWithoutBasePath(name, "inflight")
                ), name)
        );
    }

    private long readZookeeperMetric(Supplier<Long> supplier, SubscriptionName name) {
        try {
            return supplier.get();
        } catch (Exception exception) {
            logger.warn(
                    "Failed to read Zookeeper metrics for subscription: {}; root cause: {}",
                    name.getQualifiedName(), getRootCauseMessage(exception)
            );
            return -1;
        }
    }

    private String metricPath(SubscriptionName name) {
        return String.format(SUBSCRIPTION_RATE_PATTERN,
                metricsPaths.prefix(), subscriptionNameToPath(name)
        );
    }

    private String metricPathThroughput(SubscriptionName name) {
        return String.format(SUBSCRIPTION_THROUGHPUT_PATTERN,
                metricsPaths.prefix(), subscriptionNameToPath(name)
        );
    }

    private String metricPathHttpStatuses(SubscriptionName name, String statusCodeClass) {
        return String.format(SUBSCRIPTION_HTTP_STATUSES_PATTERN,
                metricsPaths.prefix(), subscriptionNameToPath(name), statusCodeClass
        );
    }

    private String metricPathTimeouts(SubscriptionName name) {
        return String.format(SUBSCRIPTION_ERROR_TIMEOUT_PATTERN,
                metricsPaths.prefix(), subscriptionNameToPath(name)
        );
    }

    private String metricPathOtherErrors(SubscriptionName name) {
        return String.format(SUBSCRIPTION_ERROR_OTHER_PATTERN,
                metricsPaths.prefix(), subscriptionNameToPath(name)
        );
    }

    private String metricPathBatchRate(SubscriptionName name) {
        return String.format(SUBSCRIPTION_BATCH_RATE_PATTERN,
                metricsPaths.prefix(), subscriptionNameToPath(name)
        );
    }

    private String subscriptionNameToPath(SubscriptionName name) {
        return String.format(SUBSCRIPTION_PATH,
                escapeDots(name.getTopicName().getGroupName()), name.getTopicName().getName(), escapeDots(name.getName())
        );
    }

    private static class ZookeeperMetrics {

        final long delivered;

        final long discarded;

        final long inflight;

        ZookeeperMetrics(long delivered, long discarded, long inflight) {
            this.delivered = delivered;
            this.discarded = discarded;
            this.inflight = inflight;
        }
    }
}
