package pl.allegro.tech.hermes.frontend.cache.topic;

import java.util.List;
import java.util.Optional;
import pl.allegro.tech.hermes.frontend.metric.CachedTopic;

public interface TopicsCache {

  Optional<CachedTopic> getTopic(String qualifiedTopicName);

  List<CachedTopic> getTopics();

  void start();
}
