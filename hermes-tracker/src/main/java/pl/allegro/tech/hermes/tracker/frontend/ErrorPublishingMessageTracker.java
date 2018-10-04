package pl.allegro.tech.hermes.tracker.frontend;

import pl.allegro.tech.hermes.api.TopicName;

import java.time.Clock;
import java.util.List;

public class ErrorPublishingMessageTracker implements PublishingTracker {

    private final List<LogRepository> repositories;
    private final Clock clock;

    public ErrorPublishingMessageTracker(List<LogRepository> repositories, Clock clock) {
        this.repositories = repositories;
        this.clock = clock;
    }

    @Override
    public void logInflight(String messageId, TopicName topicName, String hostname) {
    }

    @Override
    public void logPublished(String messageId, TopicName topicName, String hostname) {
    }

    @Override
    public void logError(final String messageId, final TopicName topicName, final String reason, final String hostname) {
        repositories.forEach(r -> r.logError(messageId, clock.millis(), topicName.qualifiedName(), reason, hostname));
    }

    public void add(LogRepository logRepository) {
        repositories.add(logRepository);
    }
}
