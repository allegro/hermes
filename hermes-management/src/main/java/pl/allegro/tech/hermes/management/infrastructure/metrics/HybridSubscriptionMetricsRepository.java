package pl.allegro.tech.hermes.management.infrastructure.metrics;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.SubscriptionMetrics;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.counter.DistributedEphemeralCounter;
import pl.allegro.tech.hermes.infrastructure.zookeeper.counter.SharedCounter;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionLagSource;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionMetricsRepository;
import pl.allegro.tech.hermes.management.infrastructure.graphite.GraphiteClient;
import pl.allegro.tech.hermes.management.infrastructure.graphite.GraphiteMetrics;
import pl.allegro.tech.hermes.management.stub.MetricsPaths;

import static pl.allegro.tech.hermes.common.metric.HermesMetrics.escapeDots;

@Component
public class HybridSubscriptionMetricsRepository implements SubscriptionMetricsRepository {

    private static final String SUBSCRIPTION_RATE_PATTERN = "sumSeries(%s.consumer.*.meter.%s.%s.%s.m1_rate)";

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
        String rateMetric = metricPath(topicName, subscriptionName);

        GraphiteMetrics metrics = graphiteClient.readMetrics(rateMetric);

        return SubscriptionMetrics.Builder.subscriptionMetrics()
                .withRate(metrics.metricValue(rateMetric))
                .withDelivered(sharedCounter.getValue(zookeeperPaths.subscriptionMetricPath(topicName, subscriptionName, "delivered")))
                .withDiscarded(sharedCounter.getValue(zookeeperPaths.subscriptionMetricPath(topicName, subscriptionName, "discarded")))
                .withInflight(distributedCounter.getValue(
                        zookeeperPaths.consumersPath(),
                        zookeeperPaths.subscriptionMetricPathWithoutBasePath(topicName, subscriptionName, "inflight")
                ))
                .withLag(lagSource.getLag(topicName, subscriptionName))
                .build();
    }

    private String metricPath(TopicName topicName, String subscriptionName) {
        return String.format(SUBSCRIPTION_RATE_PATTERN,
                metricsPaths.prefix(), escapeDots(topicName.getGroupName()), topicName.getName(), escapeDots(subscriptionName)
        );
    }
}
