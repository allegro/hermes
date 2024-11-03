package pl.allegro.tech.hermes.management.domain.detection;

public interface UnusedTopicsRepository {
  void markAsUnused(UnusedTopic unusedTopic);

  void unmarkAsUnused(UnusedTopic unusedTopic);
}
