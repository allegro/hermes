package pl.allegro.tech.hermes.frontend.message.tracker;

import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.time.Clock;

import javax.inject.Inject;

public class PublishingMessageTracker implements PublishingTracker {
    
    private final LogRepository repository;
    private final Clock clock;

    @Inject
    public PublishingMessageTracker(LogRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public void logInflight(final String messageId, final TopicName topicName) {
        repository.logInflight(messageId, clock.getTime(), topicName.qualifiedName());
    }

    @Override
    public void logPublished(final String messageId, final TopicName topicName) {
        repository.logPublished(messageId, clock.getTime(), topicName.qualifiedName());
    }

    @Override
    public void logError(final String messageId, final TopicName topicName, final String reason) {
        repository.logError(messageId, clock.getTime(), topicName.qualifiedName(), reason);
    }
}
