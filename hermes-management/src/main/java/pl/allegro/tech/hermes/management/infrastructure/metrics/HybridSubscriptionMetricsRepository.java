package pl.allegro.tech.hermes.management.infrastructure.metrics;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.SubscriptionMetrics;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.infrastructure.zookeeper.counter.DistributedEphemeralCounter;
import pl.allegro.tech.hermes.infrastructure.zookeeper.counter.SharedCounter;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionMetricsRepository;
import pl.allegro.tech.hermes.management.infrastructure.graphite.GraphiteClient;
import pl.allegro.tech.hermes.management.infrastructure.graphite.GraphiteMetrics;
import pl.allegro.tech.hermes.management.stub.MetricsPaths;

import static pl.allegro.tech.hermes.common.metric.HermesMetrics.escapeDots;

@Component
public class HybridSubscriptionMetricsRepository implements SubscriptionMetricsRepository {

    private static final String SUBSCRIPTION_RATE_PATTERN = "sumSeries(%s.consumer.*.meter.%s.%s.%s.m1_rate)";
    private static final String SUBSCRIPTION_HTTP_STATUSES_PATTERN = "sumSeries(%s.consumer.*.status.%s.%s.%s.%s.m1_rate)";
    private static final String SUBSCRIPTION_ERROR_TIMEOUT_PATTERN = "sumSeries(%s.consumer.*.status.%s.%s.%s.errors.timeout.m1_rate)";
    private static final String SUBSCRIPTION_ERROR_OTHER_PATTERN = "sumSeries(%s.consumer.*.status.%s.%s.%s.errors.other.m1_rate)";

    private final GraphiteClient graphiteClient;

    private final MetricsPaths metricsPaths;

    private final SharedCounter sharedCounter;

    private final DistributedEphemeralCounter distributedCounter;

    private final ZookeeperPaths zookeeperPaths;

    @Autowired
    public HybridSubscriptionMetricsRepository(GraphiteClient graphiteClient, MetricsPaths metricsPaths,
                                               SharedCounter sharedCounter, DistributedEphemeralCounter distributedCounter,
                                               ZookeeperPaths zookeeperPaths) {
        this.graphiteClient = graphiteClient;
        this.metricsPaths = metricsPaths;
        this.sharedCounter = sharedCounter;
        this.distributedCounter = distributedCounter;
        this.zookeeperPaths = zookeeperPaths;
    }

    @Override
    public SubscriptionMetrics loadMetrics(TopicName topicName, String subscriptionName) {
        String rateMetric = metricPath(topicName, subscriptionName);
        String timeouts = metricPathTimeouts(topicName, subscriptionName);
        String otherErrors = metricPathOtherErrors(topicName, subscriptionName);
        String codes2xxPath = metricPathHttpStatuses(topicName, subscriptionName, "2xx");
        String codes4xxPath = metricPathHttpStatuses(topicName, subscriptionName, "4xx");
        String codes5xxPath = metricPathHttpStatuses(topicName, subscriptionName, "5xx");

        GraphiteMetrics metrics = graphiteClient.readMetrics(codes2xxPath, codes4xxPath, codes5xxPath, rateMetric, timeouts, otherErrors);

        return SubscriptionMetrics.Builder.subscriptionMetrics()
                .withRate(metrics.metricValue(rateMetric))
                .withDelivered(sharedCounter.getValue(zookeeperPaths.subscriptionMetricPath(topicName, subscriptionName, "delivered")))
                .withDiscarded(sharedCounter.getValue(zookeeperPaths.subscriptionMetricPath(topicName, subscriptionName, "discarded")))
                .withInflight(distributedCounter.getValue(
                        zookeeperPaths.consumersPath(),
                        zookeeperPaths.subscriptionMetricPathWithoutBasePath(topicName, subscriptionName, "inflight")
                ))
                .withCodes2xx(metrics.metricValue(codes2xxPath))
                .withCodes4xx(metrics.metricValue(codes4xxPath))
                .withCodes5xx(metrics.metricValue(codes5xxPath))
                .withTimeouts(metrics.metricValue(timeouts))
                .withOtherErrors(metrics.metricValue(otherErrors))
                .build();
    }

    private String metricPath(TopicName topicName, String subscriptionName) {
        return String.format(SUBSCRIPTION_RATE_PATTERN,
                metricsPaths.prefix(), escapeDots(topicName.getGroupName()), topicName.getName(), escapeDots(subscriptionName)
        );
    }

    private String metricPathHttpStatuses(TopicName topicName, String subscriptionName, String statusCodeClass) {
        return String.format(SUBSCRIPTION_HTTP_STATUSES_PATTERN,
                metricsPaths.prefix(), escapeDots(topicName.getGroupName()), topicName.getName(), escapeDots(subscriptionName), statusCodeClass
        );
    }

    private String metricPathTimeouts(TopicName topicName, String subscriptionName) {
        return String.format(SUBSCRIPTION_ERROR_TIMEOUT_PATTERN,
                metricsPaths.prefix(), escapeDots(topicName.getGroupName()), topicName.getName(), escapeDots(subscriptionName)
        );
    }

    private String metricPathOtherErrors(TopicName topicName, String subscriptionName) {
        return String.format(SUBSCRIPTION_ERROR_OTHER_PATTERN,
                metricsPaths.prefix(), escapeDots(topicName.getGroupName()), topicName.getName(), escapeDots(subscriptionName)
        );
    }
}
