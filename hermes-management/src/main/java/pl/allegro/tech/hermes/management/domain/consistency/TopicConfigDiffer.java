package pl.allegro.tech.hermes.management.domain.consistency;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.kafka.common.config.TopicConfig;
import pl.allegro.tech.hermes.api.KafkaTopicConfigDiff;
import pl.allegro.tech.hermes.management.infrastructure.kafka.service.OwnedTopicConfig;

public class TopicConfigDiffer {

  public List<KafkaTopicConfigDiff> diff(Map<String, String> desired, Map<String, String> actual) {
    List<KafkaTopicConfigDiff> diffs = new ArrayList<>();
    for (String key : OwnedTopicConfig.KEYS) {
      String expected = desired.get(key);
      String current = actual.get(key);
      if (!valuesEqual(key, expected, current)) {
        diffs.add(new KafkaTopicConfigDiff(key, expected, current));
      }
    }
    return diffs;
  }

  private boolean valuesEqual(String key, String expected, String actual) {
    if (expected == null || actual == null) {
      return expected == null && actual == null;
    }
    try {
      return switch (key) {
        case TopicConfig.RETENTION_MS_CONFIG, TopicConfig.MAX_MESSAGE_BYTES_CONFIG ->
            Long.parseLong(expected.trim()) == Long.parseLong(actual.trim());
        case TopicConfig.UNCLEAN_LEADER_ELECTION_ENABLE_CONFIG ->
            isBoolean(expected)
                && isBoolean(actual)
                && Boolean.parseBoolean(expected.trim()) == Boolean.parseBoolean(actual.trim());
        default -> expected.equals(actual);
      };
    } catch (NumberFormatException ignored) {
      return false;
    }
  }

  private boolean isBoolean(String value) {
    return "true".equalsIgnoreCase(value.trim()) || "false".equalsIgnoreCase(value.trim());
  }
}
