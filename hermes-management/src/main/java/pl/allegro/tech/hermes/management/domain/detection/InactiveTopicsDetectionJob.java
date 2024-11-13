package pl.allegro.tech.hermes.management.domain.detection;

import static java.util.stream.Collectors.groupingBy;

import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;

@Component
public class InactiveTopicsDetectionJob {
  private final TopicService topicService;
  private final InactiveTopicsStorageService inactiveTopicsStorageService;
  private final InactiveTopicsDetectionService inactiveTopicsDetectionService;
  private final Optional<InactiveTopicsNotifier> notifier;
  private final Clock clock;

  private static final Logger logger = LoggerFactory.getLogger(InactiveTopicsDetectionJob.class);

  public InactiveTopicsDetectionJob(
      TopicService topicService,
      InactiveTopicsStorageService inactiveTopicsStorageService,
      InactiveTopicsDetectionService inactiveTopicsDetectionService,
      Optional<InactiveTopicsNotifier> notifier,
      Clock clock) {
    this.topicService = topicService;
    this.inactiveTopicsStorageService = inactiveTopicsStorageService;
    this.inactiveTopicsDetectionService = inactiveTopicsDetectionService;
    this.clock = clock;
    if (notifier.isEmpty()) {
      logger.info("Inactive topics notifier bean is absent");
    }
    this.notifier = notifier;
  }

  public void detectAndNotify() {
    List<String> qualifiedTopicNames = topicService.listQualifiedTopicNames();
    List<InactiveTopic> historicalInactiveTopics = inactiveTopicsStorageService.getInactiveTopics();
    List<InactiveTopic> foundInactiveTopics =
        detectInactiveTopics(qualifiedTopicNames, historicalInactiveTopics);

    Map<Boolean, List<InactiveTopic>> groupedByNeedOfNotification =
        foundInactiveTopics.stream()
            .collect(groupingBy(inactiveTopicsDetectionService::shouldBeNotified));

    List<InactiveTopic> topicsToNotify = groupedByNeedOfNotification.getOrDefault(true, List.of());
    List<InactiveTopic> topicsToSkipNotification =
        groupedByNeedOfNotification.getOrDefault(false, List.of());
    List<InactiveTopic> notifiedTopics = notify(topicsToNotify);

    saveInactiveTopics(notifiedTopics, topicsToSkipNotification);
  }

  private List<InactiveTopic> detectInactiveTopics(
      List<String> qualifiedTopicNames, List<InactiveTopic> historicalInactiveTopics) {
    Map<String, InactiveTopic> historicalInactiveTopicsByName =
        groupByName(historicalInactiveTopics);
    return qualifiedTopicNames.stream()
        .map(
            qualifiedTopicName ->
                inactiveTopicsDetectionService.detectInactiveTopic(
                    TopicName.fromQualifiedName(qualifiedTopicName),
                    Optional.ofNullable(historicalInactiveTopicsByName.get(qualifiedTopicName))))
        .map(opt -> opt.orElse(null))
        .filter(Objects::nonNull)
        .toList();
  }

  private Map<String, InactiveTopic> groupByName(List<InactiveTopic> inactiveTopics) {
    return inactiveTopics.stream()
        .collect(Collectors.toMap(InactiveTopic::qualifiedTopicName, v -> v, (v1, v2) -> v1));
  }

  private List<InactiveTopic> notify(List<InactiveTopic> inactiveTopics) {
    if (inactiveTopics.isEmpty()) {
      logger.info("No inactive topics to notify");
      return inactiveTopics;
    } else if (notifier.isPresent()) {
      logger.info("Notifying {} inactive topics", inactiveTopics.size());
      notifier.get().notify(inactiveTopics);
      Instant now = clock.instant();
      return inactiveTopics.stream().map(topic -> topic.notificationSent(now)).toList();
    } else {
      logger.info("Skipping notification of {} inactive topics", inactiveTopics.size());
      return inactiveTopics;
    }
  }

  private void saveInactiveTopics(
      List<InactiveTopic> notifiedTopics, List<InactiveTopic> skippedNotificationTopics) {
    inactiveTopicsStorageService.markAsInactive(
        Stream.concat(notifiedTopics.stream(), skippedNotificationTopics.stream()).toList());
  }
}
