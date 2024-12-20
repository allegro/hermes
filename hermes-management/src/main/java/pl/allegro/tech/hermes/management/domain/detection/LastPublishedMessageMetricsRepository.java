package pl.allegro.tech.hermes.management.domain.detection;

import java.time.Instant;
import java.util.Optional;
import pl.allegro.tech.hermes.api.TopicName;

public interface LastPublishedMessageMetricsRepository {
  Optional<Instant> getLastPublishedMessageTimestamp(TopicName topicName);
}
