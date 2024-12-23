package pl.allegro.tech.hermes.management.domain.detection;

import static java.util.stream.Collectors.groupingBy;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.OwnerId;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.management.config.detection.InactiveTopicsDetectionProperties;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;

@Component
public class InactiveTopicsDetectionJob {
  private final TopicService topicService;
  private final InactiveTopicsStorageService inactiveTopicsStorageService;
  private final InactiveTopicsDetectionService inactiveTopicsDetectionService;
  private final Optional<InactiveTopicsNotifier> notifier;
  private final InactiveTopicsDetectionProperties properties;
  private final Clock clock;
  private final MeterRegistry meterRegistry;

  private static final Logger logger = LoggerFactory.getLogger(InactiveTopicsDetectionJob.class);

  public InactiveTopicsDetectionJob(
      TopicService topicService,
      InactiveTopicsStorageService inactiveTopicsStorageService,
      InactiveTopicsDetectionService inactiveTopicsDetectionService,
      Optional<InactiveTopicsNotifier> notifier,
      InactiveTopicsDetectionProperties properties,
      Clock clock,
      MeterRegistry meterRegistry) {
    this.topicService = topicService;
    this.inactiveTopicsStorageService = inactiveTopicsStorageService;
    this.inactiveTopicsDetectionService = inactiveTopicsDetectionService;
    this.properties = properties;
    this.clock = clock;
    this.meterRegistry = meterRegistry;
    if (notifier.isEmpty()) {
      logger.info("Inactive topics notifier bean is absent");
    }
    this.notifier = notifier;
  }

  public void detectAndNotify() {
    List<Topic> topics = topicService.getAllTopics();
    List<String> qualifiedTopicNames = topics.stream().map(Topic::getQualifiedName).toList();
    List<InactiveTopic> historicalInactiveTopics = inactiveTopicsStorageService.getInactiveTopics();
    List<InactiveTopic> foundInactiveTopics =
        detectInactiveTopics(qualifiedTopicNames, historicalInactiveTopics);

    Map<Boolean, List<InactiveTopic>> groupedByNeedOfNotification =
        foundInactiveTopics.stream()
            .collect(groupingBy(inactiveTopicsDetectionService::shouldBeNotified));

    List<InactiveTopic> topicsToNotify = groupedByNeedOfNotification.getOrDefault(true, List.of());
    List<InactiveTopic> topicsToSkipNotification =
        groupedByNeedOfNotification.getOrDefault(false, List.of());
    List<InactiveTopic> notifiedTopics = notify(enrichWithOwner(topicsToNotify, topics));

    List<InactiveTopic> processedTopics =
        limitHistory(
            Stream.concat(notifiedTopics.stream(), topicsToSkipNotification.stream()).toList());
    measureInactiveTopics(processedTopics);
    inactiveTopicsStorageService.markAsInactive(processedTopics);
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

  private List<InactiveTopicWithOwner> enrichWithOwner(
      List<InactiveTopic> inactiveTopics, List<Topic> topics) {
    Map<String, OwnerId> ownerByTopicName = new HashMap<>();
    topics.forEach(topic -> ownerByTopicName.put(topic.getQualifiedName(), topic.getOwner()));

    return inactiveTopics.stream()
        .map(
            inactiveTopic ->
                new InactiveTopicWithOwner(
                    inactiveTopic, ownerByTopicName.get(inactiveTopic.qualifiedTopicName())))
        .toList();
  }

  private List<InactiveTopic> notify(List<InactiveTopicWithOwner> inactiveTopics) {
    if (inactiveTopics.isEmpty()) {
      logger.info("No inactive topics to notify");
      return List.of();
    } else if (notifier.isPresent()) {
      logger.info("Notifying {} inactive topics", inactiveTopics.size());
      NotificationResult result = notifier.get().notify(inactiveTopics);
      Instant now = clock.instant();

      return inactiveTopics.stream()
          .map(InactiveTopicWithOwner::topic)
          .map(
              topic ->
                  result.isSuccess(topic.qualifiedTopicName())
                      ? topic.notificationSent(now)
                      : topic)
          .toList();
    } else {
      logger.info("Skipping notification of {} inactive topics", inactiveTopics.size());
      return inactiveTopics.stream().map(InactiveTopicWithOwner::topic).toList();
    }
  }

  private List<InactiveTopic> limitHistory(List<InactiveTopic> inactiveTopics) {
    return inactiveTopics.stream()
        .map(topic -> topic.limitNotificationsHistory(properties.notificationsHistoryLimit()))
        .toList();
  }

  private void measureInactiveTopics(List<InactiveTopic> processedTopics) {
    processedTopics.stream()
        .collect(
            Collectors.groupingBy(
                topic -> topic.notificationTimestampsMs().size(), Collectors.counting()))
        .forEach(
            (notificationsCount, topicsCount) -> {
              Tags tags = Tags.of("notifications", notificationsCount.toString());
              meterRegistry.gauge("inactive-topics", tags, topicsCount);
            });
  }
}
