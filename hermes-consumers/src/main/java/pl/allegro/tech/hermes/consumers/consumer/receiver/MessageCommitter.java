package pl.allegro.tech.hermes.consumers.consumer.receiver;

import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetsToCommit;

public interface MessageCommitter {
    void commitOffsets(OffsetsToCommit offsetsToCommit);
}
