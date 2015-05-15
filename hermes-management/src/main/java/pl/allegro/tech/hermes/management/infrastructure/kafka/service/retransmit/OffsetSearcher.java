package pl.allegro.tech.hermes.management.infrastructure.kafka.service.retransmit;

import com.google.common.collect.Range;

import java.util.Optional;

class OffsetSearcher {

    private final KafkaTimestampExtractor timestampExtractor;

    OffsetSearcher(KafkaTimestampExtractor timestampExtractor) {
        this.timestampExtractor = timestampExtractor;
    }

    long search(Range<Long> offsetRange, long timestamp) {
        long left = offsetRange.lowerEndpoint() - 1;
        long right = offsetRange.upperEndpoint();

        while (left + 1 < right) {
            long half = (left + right) / 2;
            Optional<Long> extractedTimestamp = timestampExtractor.extract(half);

            if (!extractedTimestamp.isPresent() || timestamp > extractedTimestamp.get()) {
                left = half;
            } else {
                right = half;
            }
        }
        return right;
    }
}
