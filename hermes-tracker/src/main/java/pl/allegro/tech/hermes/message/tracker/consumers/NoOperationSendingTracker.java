package pl.allegro.tech.hermes.message.tracker.consumers;

public class NoOperationSendingTracker implements SendingTracker {

    @Override
    public void logSent(MessageMetadata message) {

    }

    @Override
    public void logFailed(MessageMetadata message, String reason) {

    }

    @Override
    public void logDiscarded(MessageMetadata message, String reason) {

    }

    @Override
    public void logInflight(MessageMetadata message) {

    }
}
