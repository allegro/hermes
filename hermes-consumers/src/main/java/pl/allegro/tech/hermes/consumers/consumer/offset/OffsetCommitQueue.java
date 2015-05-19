package pl.allegro.tech.hermes.consumers.consumer.offset;

import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

final class OffsetCommitQueue {

    private final NavigableMap<Long, Boolean> offsets = new TreeMap<>();

    private final OffsetCommitQueueMonitor monitor;

    OffsetCommitQueue(OffsetCommitQueueMonitor monitor) {
        this.monitor = monitor;
    }

    synchronized void put(long offset) {
        offsets.put(offset, false);
    }

    synchronized void markDelivered(long offset) {
        offsets.put(offset, true);
    }

    synchronized Optional<Long> poll() {
        Long offsetToCommit = null;

        if (firstOffsetDelivered()) {
            do {

                offsetToCommit = offsets.pollFirstEntry().getKey();

            } while (firstOffsetDelivered());

            monitor.newOffsetCommit();
        } else if (!empty()) {
            monitor.nothingNewToCommit(offsets.size(), offsets.firstEntry().getKey());
        }

        return Optional.ofNullable(offsetToCommit);
    }

    private boolean firstOffsetDelivered() {
       return !empty() && offsets.firstEntry().getValue();
    }

    private boolean empty() {
        return offsets.isEmpty();
    }
}
