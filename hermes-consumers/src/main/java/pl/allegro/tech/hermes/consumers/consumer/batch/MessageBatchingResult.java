package pl.allegro.tech.hermes.consumers.consumer.batch;

import pl.allegro.tech.hermes.tracker.consumers.MessageMetadata;

import java.util.List;

public class MessageBatchingResult {
    private final MessageBatch batch;
    private final List<MessageMetadata> discarded;

    public MessageBatchingResult(MessageBatch batch, List<MessageMetadata> discarded) {
        this.batch = batch;
        this.discarded = discarded;
    }

    public MessageBatch getBatch() {
        return batch;
    }

    public List<MessageMetadata> getDiscarded() {
        return discarded;
    }
}
