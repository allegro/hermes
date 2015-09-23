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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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

        GraphiteMetrics metrics = graphiteClient.readMetrics(rateMetric);

        List<String> httpStatusCodes = allHttpStatusCodeClasses()
                .map(code -> metricPathHttpStatuses(topicName, subscriptionName, code))
                .map(m -> graphiteClient.readMetrics(m)
                .metricValue(m))
                .collect(Collectors.toList());

        return SubscriptionMetrics.Builder.subscriptionMetrics()
                .withRate(metrics.metricValue(rateMetric))
                .withDelivered(sharedCounter.getValue(zookeeperPaths.subscriptionMetricPath(topicName, subscriptionName, "delivered")))
                .withDiscarded(sharedCounter.getValue(zookeeperPaths.subscriptionMetricPath(topicName, subscriptionName, "discarded")))
                .withInflight(distributedCounter.getValue(
                        zookeeperPaths.consumersPath(),
                        zookeeperPaths.subscriptionMetricPathWithoutBasePath(topicName, subscriptionName, "inflight")
                ))
                .withHttpStatusCodesM1(httpStatusCodes)
                .withTimeoutsM1(graphiteClient.readMetrics(timeouts).metricValue(timeouts))
                .withOtherErrorsM1(graphiteClient.readMetrics(otherErrors).metricValue(otherErrors))
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

    private Stream<String> allHttpStatusCodeClasses() {
        return IntStream.range(1, 6).mapToObj(i -> i + "xx");
    }
}
