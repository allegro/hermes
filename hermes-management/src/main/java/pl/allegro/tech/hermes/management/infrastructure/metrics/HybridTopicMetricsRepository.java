package pl.allegro.tech.hermes.management.infrastructure.metrics;

import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.TopicMetrics;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.domain.subscription.SubscriptionRepository;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.management.domain.topic.TopicMetricsRepository;

@Component
public class HybridTopicMetricsRepository implements TopicMetricsRepository {

  private final MonitoringTopicMetricsProvider monitoringTopicMetricsProvider;

  private final SummedSharedCounter summedSharedCounter;

  private final ZookeeperPaths zookeeperPaths;

  private final SubscriptionRepository subscriptionRepository;

  public HybridTopicMetricsRepository(
      MonitoringTopicMetricsProvider monitoringTopicMetricsProvider,
      SummedSharedCounter summedSharedCounter,
      ZookeeperPaths zookeeperPaths,
      SubscriptionRepository subscriptionRepository) {
    this.monitoringTopicMetricsProvider = monitoringTopicMetricsProvider;
    this.summedSharedCounter = summedSharedCounter;
    this.zookeeperPaths = zookeeperPaths;
    this.subscriptionRepository = subscriptionRepository;
  }

  @Override
  public TopicMetrics loadMetrics(TopicName topicName) {
    MonitoringTopicMetricsProvider.MonitoringTopicMetrics metrics =
        monitoringTopicMetricsProvider.topicMetrics(topicName);

    return TopicMetrics.Builder.topicMetrics()
        .withRate(metrics.rate())
        .withDeliveryRate(metrics.deliveryRate())
        .withThroughput(metrics.throughput())
        .withPublished(
            summedSharedCounter.getValue(zookeeperPaths.topicMetricPath(topicName, "published")))
        .withVolume(
            summedSharedCounter.getValue(zookeeperPaths.topicMetricPath(topicName, "volume")))
        .withSubscriptions(subscriptionRepository.listSubscriptionNames(topicName).size())
        .build();
  }
}
