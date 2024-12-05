package pl.allegro.tech.hermes.management.domain.detection;

import java.util.List;

public interface InactiveTopicsRepository {
  void upsert(List<InactiveTopic> inactiveTopics);

  List<InactiveTopic> read();
}
