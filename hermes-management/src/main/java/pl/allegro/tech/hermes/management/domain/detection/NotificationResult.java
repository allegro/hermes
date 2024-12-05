package pl.allegro.tech.hermes.management.domain.detection;

import java.util.Map;
import java.util.Optional;

public class NotificationResult {
  private final Map<String, Boolean> qualifiedTopicNameToSuccess;

  public NotificationResult(Map<String, Boolean> qualifiedTopicNameToSuccess) {
    this.qualifiedTopicNameToSuccess = qualifiedTopicNameToSuccess;
  }

  public boolean isSuccess(String qualifiedTopicName) {
    return Optional.ofNullable(qualifiedTopicNameToSuccess.get(qualifiedTopicName)).orElse(false);
  }
}
