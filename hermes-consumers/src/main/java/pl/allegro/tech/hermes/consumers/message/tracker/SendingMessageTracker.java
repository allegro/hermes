package pl.allegro.tech.hermes.consumers.message.tracker;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.time.Clock;
import pl.allegro.tech.hermes.consumers.consumer.receiver.Message;

import javax.inject.Inject;

public class SendingMessageTracker implements SendingTracker {
    private final LogRepository repository;
    private final Clock clock;

    @Inject
    SendingMessageTracker(LogRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public void logSent(final Message message, final Subscription subscription) {
        repository.logSuccessful(message, clock.getTime(), subscription.getQualifiedTopicName(), subscription.getName());
    }

    @Override
    public void logFailed(final Message message, final Subscription subscription, final String reason) {
        repository.logFailed(message, clock.getTime(), subscription.getQualifiedTopicName(), subscription.getName(), reason);
    }

    @Override
    public void logDiscarded(final Message message, final Subscription subscription, final String reason) {
        repository.logDiscarded(message, clock.getTime(), subscription.getQualifiedTopicName(), subscription.getName(), reason);
    }

    @Override
    public void logInflight(Message message, Subscription subscription) {
        repository.logInflight(message, clock.getTime(), subscription.getQualifiedTopicName(), subscription.getName());
    }
}
