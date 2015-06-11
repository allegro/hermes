package pl.allegro.tech.hermes.message.tracker.consumers;

import pl.allegro.tech.hermes.api.Subscription;

import java.time.Clock;
import java.util.List;

class SendingMessageTracker implements SendingTracker {
    private final List<LogRepository> repositories;
    private final Clock clock;

    SendingMessageTracker(List<LogRepository> repositories, Clock clock) {
        this.repositories = repositories;
        this.clock = clock;
    }

    @Override
    public void logSent(MessageMetadata message, Subscription subscription) {
        repositories.forEach(r -> r.logSuccessful(message, clock.millis(), subscription.getQualifiedTopicName(), subscription.getName()));
    }

    @Override
    public void logFailed(MessageMetadata message, Subscription subscription, final String reason) {
        repositories.forEach(r -> r.logFailed(message, clock.millis(), subscription.getQualifiedTopicName(), subscription.getName(), reason));
    }

    @Override
    public void logDiscarded(MessageMetadata message, final Subscription subscription, final String reason) {
        repositories.forEach(r ->
                r.logDiscarded(message, clock.millis(), subscription.getQualifiedTopicName(), subscription.getName(), reason));
    }

    @Override
    public void logInflight(MessageMetadata message, Subscription subscription) {
        repositories.forEach(r -> r.logInflight(message, clock.millis(), subscription.getQualifiedTopicName(), subscription.getName()));
    }

    void add(LogRepository logRepository) {
        repositories.add(logRepository);
    }
}
