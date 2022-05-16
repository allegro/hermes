package pl.allegro.tech.hermes.management.infrastructure.metrics;

import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.TopicMetrics;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.management.domain.topic.TopicMetricsRepository;
import pl.allegro.tech.hermes.management.infrastructure.graphite.GraphiteClient;
import pl.allegro.tech.hermes.management.infrastructure.graphite.GraphiteMetrics;
import pl.allegro.tech.hermes.management.stub.MetricsPaths;

import static pl.allegro.tech.hermes.common.metric.HermesMetrics.escapeDots;

@Component
public class HybridTopicMetricsRepository implements TopicMetricsRepository {

    private static final String RATE_PATTERN = "sumSeries(%s.producer.*.meter.%s.%s.m1_rate)";

    private static final String DELIVERY_RATE_PATTERN = "sumSeries(%s.consumer.*.meter.%s.%s.m1_rate)";

    private static final String THROUGHPUT_PATTERN = "sumSeries(%s.producer.*.throughput.%s.%s.m1_rate)";

    private final GraphiteClient graphiteClient;

    private final MetricsPaths metricsPaths;

    private final SummedSharedCounter summedSharedCounter;

    private final ZookeeperPaths zookeeperPaths;

    private final SubscriptionRepository subscriptionRepository;

    public HybridTopicMetricsRepository(GraphiteClient graphiteClient, MetricsPaths metricsPaths,
                                        SummedSharedCounter summedSharedCounter, ZookeeperPaths zookeeperPaths,
                                        SubscriptionRepository subscriptionRepository) {
        this.graphiteClient = graphiteClient;
        this.metricsPaths = metricsPaths;
        this.summedSharedCounter = summedSharedCounter;
        this.zookeeperPaths = zookeeperPaths;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    public TopicMetrics loadMetrics(TopicName topicName) {
        String rateMetric = metricPath(RATE_PATTERN, topicName);
        String deliveryRateMetric = metricPath(DELIVERY_RATE_PATTERN, topicName);
        String throughputMetric = metricPath(THROUGHPUT_PATTERN, topicName);

        GraphiteMetrics metrics = graphiteClient.readMetrics(rateMetric, deliveryRateMetric);

        return TopicMetrics.Builder.topicMetrics()
                .withRate(metrics.metricValue(rateMetric))
                .withDeliveryRate(metrics.metricValue(deliveryRateMetric))
                .withPublished(summedSharedCounter.getValue(zookeeperPaths.topicMetricPath(topicName, "published")))
                .withVolume(summedSharedCounter.getValue(zookeeperPaths.topicMetricPath(topicName, "volume")))
                .withSubscriptions(subscriptionRepository.listSubscriptionNames(topicName).size())
                .withThroughput(metrics.metricValue(throughputMetric))
                .build();
    }

    private String metricPath(String pattern, TopicName topicName) {
        return String.format(pattern, metricsPaths.prefix(), escapeDots(topicName.getGroupName()), escapeDots(topicName.getName()));
    }
}
