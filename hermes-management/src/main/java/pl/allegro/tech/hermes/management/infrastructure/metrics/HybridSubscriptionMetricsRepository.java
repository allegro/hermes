package pl.allegro.tech.hermes.management.infrastructure.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.PersistentSubscriptionMetrics;
import pl.allegro.tech.hermes.api.SubscriptionMetrics;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionLagSource;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionMetricsRepository;
import pl.allegro.tech.hermes.management.infrastructure.metrics.MonitoringSubscriptionMetricsProvider.MonitoringSubscriptionMetrics;

import java.util.function.Supplier;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;


@Component
public class HybridSubscriptionMetricsRepository implements SubscriptionMetricsRepository {

    private static final Logger logger = LoggerFactory.getLogger(HybridSubscriptionMetricsRepository.class);

    private final MonitoringSubscriptionMetricsProvider monitoringSubscriptionMetricsProvider;
    private final SummedSharedCounter summedSharedCounter;
    private final ZookeeperPaths zookeeperPaths;
    private final SubscriptionLagSource lagSource;

    public HybridSubscriptionMetricsRepository(MonitoringSubscriptionMetricsProvider monitoringSubscriptionMetricsProvider,
                                               SummedSharedCounter summedSharedCounter,
                                               ZookeeperPaths zookeeperPaths, SubscriptionLagSource lagSource) {
        this.monitoringSubscriptionMetricsProvider = monitoringSubscriptionMetricsProvider;
        this.summedSharedCounter = summedSharedCounter;
        this.zookeeperPaths = zookeeperPaths;
        this.lagSource = lagSource;
    }

    @Override
    public SubscriptionMetrics loadMetrics(TopicName topicName, String subscriptionName) {
        SubscriptionName name = new SubscriptionName(subscriptionName, topicName);

        MonitoringSubscriptionMetrics monitoringMetrics = monitoringSubscriptionMetricsProvider.subscriptionMetrics(name);
        ZookeeperMetrics zookeeperMetrics = readZookeeperMetrics(name);

        return SubscriptionMetrics.Builder.subscriptionMetrics()
                .withRate(monitoringMetrics.rate())
                .withCodes2xx(monitoringMetrics.codes2xx())
                .withCodes4xx(monitoringMetrics.code4xx())
                .withCodes5xx(monitoringMetrics.code5xx())
                .withRetries(monitoringMetrics.retries())
                .withTimeouts(monitoringMetrics.timeouts())
                .withOtherErrors(monitoringMetrics.otherErrors())
                .withThroughput(monitoringMetrics.throughput())
                .withBatchRate(monitoringMetrics.metricPathBatchRate())
                .withDiscarded(zookeeperMetrics.discarded)
                .withDelivered(zookeeperMetrics.delivered)
                .withVolume(zookeeperMetrics.volume)
                .withLag(lagSource.getLag(topicName, subscriptionName))
                .build();
    }

    @Override
    public PersistentSubscriptionMetrics loadZookeeperMetrics(TopicName topicName, String subscriptionName) {
        SubscriptionName name = new SubscriptionName(subscriptionName, topicName);
        ZookeeperMetrics zookeeperMetrics = readZookeeperMetrics(name);

        return new PersistentSubscriptionMetrics(zookeeperMetrics.delivered, zookeeperMetrics.discarded, zookeeperMetrics.volume);
    }

    private ZookeeperMetrics readZookeeperMetrics(SubscriptionName name) {
        return new ZookeeperMetrics(
                readZookeeperMetric(() -> summedSharedCounter.getValue(zookeeperPaths.subscriptionMetricPath(name, "delivered")), name),
                readZookeeperMetric(() -> summedSharedCounter.getValue(zookeeperPaths.subscriptionMetricPath(name, "discarded")), name),
                readZookeeperMetric(() -> summedSharedCounter.getValue(zookeeperPaths.subscriptionMetricPath(name, "volume")), name)
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

    private record ZookeeperMetrics(long delivered, long discarded, long volume) { }
}
