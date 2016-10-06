package pl.allegro.tech.hermes.consumers.consumer.result

import pl.allegro.tech.hermes.tracker.consumers.LogRepository
import pl.allegro.tech.hermes.tracker.consumers.MessageMetadata

class InMemoryLogRepository implements LogRepository {

    private final List<MessageMetadata> successful = []
    private final List<MessageMetadata> failed = []
    private final List<MessageMetadata> discarded = []
    private final List<MessageMetadata> inflight = []
    private final List<MessageMetadata> filtered = []

    @Override
    void logSuccessful(MessageMetadata message, String hostname, long timestamp) {
        successful.add(message)
    }

    @Override
    void logFailed(MessageMetadata message, String hostname, long timestamp, String reason) {
        failed.add(message)
    }

    @Override
    void logDiscarded(MessageMetadata message, long timestamp, String reason) {
        discarded.add(message)
    }

    @Override
    void logInflight(MessageMetadata message, long timestamp) {
        inflight.add(message)
    }

    @Override
    void logFiltered(MessageMetadata message, long timestamp, String reason) {
        filtered.add(message)
    }

    boolean hasSuccessfulLog(String kafkaTopic, int partition, long offset) {
        return logContains(successful, kafkaTopic, partition, offset)
    }

    boolean hasFailedLog(String kafkaTopic, int partition, long offset) {
        return logContains(failed, kafkaTopic, partition, offset)
    }

    boolean hasDiscardedLog(String kafkaTopic, int partition, long offset) {
        return logContains(discarded, kafkaTopic, partition, offset)
    }

    boolean hasInflightLog(String kafkaTopic, int partition, long offset) {
        return logContains(inflight, kafkaTopic, partition, offset)
    }

    boolean hasFilteredLog(String kafkaTopic, int partition, long offset) {
        return logContains(filtered, kafkaTopic, partition, offset)
    }

    private boolean logContains(List<MessageMetadata> log, String kafkaTopic, int partition, long offset) {
        return log.any { m -> m.kafkaTopic == kafkaTopic && m.partition == partition && m.offset == offset }
    }
}
