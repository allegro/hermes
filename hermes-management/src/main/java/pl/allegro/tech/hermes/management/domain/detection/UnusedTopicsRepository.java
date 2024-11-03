package pl.allegro.tech.hermes.management.domain.detection;

import java.util.List;

public interface UnusedTopicsRepository {
  void upsert(List<UnusedTopic> unusedTopics);

  List<UnusedTopic> read();
}
