package pl.allegro.tech.hermes.management.infrastructure.kafka.service;

import java.util.List;
import java.util.Map;
import org.apache.kafka.common.config.TopicConfig;
import pl.allegro.tech.hermes.management.config.TopicProperties;

public final class OwnedTopicConfig {

  public static final List<String> KEYS =
      List.of(
          TopicConfig.RETENTION_MS_CONFIG,
          TopicConfig.UNCLEAN_LEADER_ELECTION_ENABLE_CONFIG,
          TopicConfig.MAX_MESSAGE_BYTES_CONFIG);

  private OwnedTopicConfig() {}

  public static Map<String, String> desiredConfig(
      long retentionPolicy, TopicProperties topicProperties) {
    return Map.of(
        TopicConfig.RETENTION_MS_CONFIG,
        String.valueOf(retentionPolicy),
        TopicConfig.UNCLEAN_LEADER_ELECTION_ENABLE_CONFIG,
        Boolean.toString(topicProperties.isUncleanLeaderElectionEnabled()),
        TopicConfig.MAX_MESSAGE_BYTES_CONFIG,
        String.valueOf(topicProperties.getMaxMessageSize()));
  }
}
