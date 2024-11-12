package pl.allegro.tech.hermes.management.domain.detection;

import java.util.List;

public interface InactiveTopicsNotifier {
  void notify(List<InactiveTopic> inactiveTopics);
}
