package pl.allegro.tech.hermes.management.domain.detection;

import pl.allegro.tech.hermes.api.TopicName;

import java.time.Instant;

public interface LastPublishedMessageMetricsRepository {
    Instant getLastPublishedMessageTimestamp(TopicName topicName);
}
