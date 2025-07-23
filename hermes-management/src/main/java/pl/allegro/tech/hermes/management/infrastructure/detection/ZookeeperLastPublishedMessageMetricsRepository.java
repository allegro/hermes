package pl.allegro.tech.hermes.management.infrastructure.detection;

import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.infrastructure.zookeeper.ZookeeperPaths;
import pl.allegro.tech.hermes.management.domain.detection.LastPublishedMessageMetricsRepository;
import pl.allegro.tech.hermes.management.infrastructure.metrics.SummedSharedCounter;

@Component
public class ZookeeperLastPublishedMessageMetricsRepository
    implements LastPublishedMessageMetricsRepository {
  private final SummedSharedCounter summedSharedCounter;
  private final ZookeeperPaths zookeeperPaths;

  public ZookeeperLastPublishedMessageMetricsRepository(
      SummedSharedCounter summedSharedCounter, ZookeeperPaths zookeeperPaths) {
    this.summedSharedCounter = summedSharedCounter;
    this.zookeeperPaths = zookeeperPaths;
  }

  @Override
  public Optional<Instant> getLastPublishedMessageTimestamp(TopicName topicName) {
    return summedSharedCounter.getLastModified(
        zookeeperPaths.topicMetricPath(topicName, "published"));
  }
}
