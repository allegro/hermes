package pl.allegro.tech.hermes.management.domain.detection;

import pl.allegro.tech.hermes.api.TopicName;

import java.time.Instant;
import java.util.Optional;

public interface LastPublishedMessageMetricsRepository {
    Optional<Instant> getLastPublishedMessageTimestamp(TopicName topicName);
}
