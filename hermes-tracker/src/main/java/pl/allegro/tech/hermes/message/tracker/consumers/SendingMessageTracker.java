package pl.allegro.tech.hermes.message.tracker.consumers;

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
    public void logSent(MessageMetadata message) {
        repositories.forEach(r -> r.logSuccessful(message, clock.millis()));
    }

    @Override
    public void logFailed(MessageMetadata message, final String reason) {
        repositories.forEach(r -> r.logFailed(message, clock.millis(), reason));
    }

    @Override
    public void logDiscarded(MessageMetadata message, final String reason) {
        repositories.forEach(r ->
                r.logDiscarded(message, clock.millis(), reason));
    }

    @Override
    public void logInflight(MessageMetadata message) {
        repositories.forEach(r -> r.logInflight(message, clock.millis()));
    }

    void add(LogRepository logRepository) {
        repositories.add(logRepository);
    }
}
