package pl.allegro.tech.hermes.tracker.frontend;

import pl.allegro.tech.hermes.api.TopicName;

import java.time.Clock;
import java.util.List;

public class PublishingMessageTracker implements PublishingTracker {

    private final List<LogRepository> repositories;
    private final Clock clock;

    public PublishingMessageTracker(List<LogRepository> repositories, Clock clock) {
        this.repositories = repositories;
        this.clock = clock;
    }

    @Override
    public void logInflight(final String messageId, final TopicName topicName) {
        repositories.forEach(r -> r.logInflight(messageId, clock.millis(), topicName.qualifiedName()));
    }

    @Override
    public void logPublished(final String messageId, final TopicName topicName) {
        repositories.forEach(r -> r.logPublished(messageId, clock.millis(), topicName.qualifiedName()));
    }

    @Override
    public void logError(final String messageId, final TopicName topicName, final String reason) {
        repositories.forEach(r -> r.logError(messageId, clock.millis(), topicName.qualifiedName(), reason));
    }

    public void add(LogRepository logRepository) {
        repositories.add(logRepository);
    }
}
