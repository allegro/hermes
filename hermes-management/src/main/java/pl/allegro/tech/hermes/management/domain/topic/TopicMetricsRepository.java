package pl.allegro.tech.hermes.management.domain.topic;

import pl.allegro.tech.hermes.api.TopicMetrics;
import pl.allegro.tech.hermes.api.TopicName;

public interface TopicMetricsRepository {

  TopicMetrics loadMetrics(TopicName topicName);
}
