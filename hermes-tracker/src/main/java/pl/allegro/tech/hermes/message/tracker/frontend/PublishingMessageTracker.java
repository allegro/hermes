package pl.allegro.tech.hermes.message.tracker.frontend;

import pl.allegro.tech.hermes.api.TopicName;

import java.time.Clock;

public class PublishingMessageTracker implements PublishingTracker {
    
    private final LogRepository repository;
    private final Clock clock;

    public PublishingMessageTracker(LogRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public void logInflight(final String messageId, final TopicName topicName) {
        repository.logInflight(messageId, clock.millis(), topicName.qualifiedName());
    }

    @Override
    public void logPublished(final String messageId, final TopicName topicName) {
        repository.logPublished(messageId, clock.millis(), topicName.qualifiedName());
    }

    @Override
    public void logError(final String messageId, final TopicName topicName, final String reason) {
        repository.logError(messageId, clock.millis(), topicName.qualifiedName(), reason);
    }
}
