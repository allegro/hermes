package pl.allegro.tech.hermes.management.domain.detection;

import static java.util.stream.Collectors.groupingBy;

import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.management.config.detection.UnusedTopicsDetectionProperties;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;

@Component
@EnableConfigurationProperties({UnusedTopicsDetectionProperties.class})
public class UnusedTopicsDetectionJob {
  private final TopicService topicService;
  private final UnusedTopicsService unusedTopicsService;
  private final UnusedTopicsDetectionService unusedTopicsDetectionService;
  private final UnusedTopicsNotifier notifier;
  private final Clock clock;

  public UnusedTopicsDetectionJob(
      TopicService topicService,
      UnusedTopicsService unusedTopicsService,
      UnusedTopicsDetectionService unusedTopicsDetectionService,
      UnusedTopicsNotifier notifier,
      Clock clock) {
    this.topicService = topicService;
    this.unusedTopicsService = unusedTopicsService;
    this.unusedTopicsDetectionService = unusedTopicsDetectionService;
    this.notifier = notifier;
    this.clock = clock;
  }

  public void detectAndNotify() {
    List<Topic> allTopics = topicService.getAllTopics();
    List<UnusedTopic> historicalUnusedTopics = unusedTopicsService.getUnusedTopics();
    List<UnusedTopic> foundUnusedTopics = detectUnusedTopics(allTopics, historicalUnusedTopics);

    Map<Boolean, List<UnusedTopic>> groupedByNeedOfNotification =
        foundUnusedTopics.stream()
            .collect(groupingBy(unusedTopicsDetectionService::shouldBeNotified));

    notifier.notify(groupedByNeedOfNotification.get(true));

    Instant now = clock.instant();
    List<UnusedTopic> unusedTopicsToSave =
        Stream.concat(
                groupedByNeedOfNotification.get(true).stream()
                    .map(topic -> topic.notificationSent(now)),
                groupedByNeedOfNotification.get(false).stream())
            .toList();

    unusedTopicsService.markAsUnused(unusedTopicsToSave);
  }

  private List<UnusedTopic> detectUnusedTopics(
      List<Topic> allTopics, List<UnusedTopic> historicalUnusedTopics) {
    Map<String, UnusedTopic> historicalUnusedTopicsByName = groupByName(historicalUnusedTopics);
    return allTopics.stream()
        .map(
            topic ->
                unusedTopicsDetectionService.detectUnusedTopic(
                    topic.getName(),
                    Optional.ofNullable(
                        historicalUnusedTopicsByName.get(topic.getQualifiedName()))))
        .map(opt -> opt.orElse(null))
        .filter(Objects::nonNull)
        .toList();
  }

  private Map<String, UnusedTopic> groupByName(List<UnusedTopic> unusedTopics) {
    return unusedTopics.stream()
        .collect(Collectors.toMap(UnusedTopic::qualifiedTopicName, v -> v, (v1, v2) -> v1));
  }
}
