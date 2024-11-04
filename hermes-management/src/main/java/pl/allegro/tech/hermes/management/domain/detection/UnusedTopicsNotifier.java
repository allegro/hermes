package pl.allegro.tech.hermes.management.domain.detection;

import java.util.List;

public interface UnusedTopicsNotifier {
  void notify(List<UnusedTopic> unusedTopics);
}
