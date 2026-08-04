package pl.allegro.tech.hermes.management.domain.topic;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.kafka.KafkaTopic;

public interface BrokerTopicManagement {

  void createTopic(Topic topic);

  void createTopic(Topic topic, KafkaTopic kafkaTopic);

  void removeTopic(Topic topic);

  void updateTopic(Topic topic);

  void updateTopicConfig(KafkaTopic kafkaTopic, Map<String, String> config);

  boolean topicExists(Topic topic);

  boolean topicExists(KafkaTopic kafkaTopic);

  Set<String> listTopicNames();

  Map<KafkaTopic, Map<String, String>> readTopicConfigs(Collection<KafkaTopic> kafkaTopics);
}
