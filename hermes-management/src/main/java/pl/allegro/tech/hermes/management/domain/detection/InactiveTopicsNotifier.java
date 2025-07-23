package pl.allegro.tech.hermes.management.domain.detection;

import java.util.List;

public interface InactiveTopicsNotifier {
  NotificationResult notify(List<InactiveTopicWithOwner> inactiveTopics);
}
