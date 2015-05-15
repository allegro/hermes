package pl.allegro.tech.hermes.management.infrastructure.kafka.service.retransmit

import com.google.common.collect.Range
import spock.lang.Specification

class OffsetSearcherTest extends Specification {

    private KafkaTimestampExtractor extractor = Stub(KafkaTimestampExtractor)
    
    private OffsetSearcher searcher = new OffsetSearcher(extractor)

    def "should find offset for given timestamp"() {
        given:
        extractor.extract(4L) >> Optional.of(800L)
        extractor.extract(7L) >> Optional.of(1100L)
        extractor.extract(5L) >> Optional.of(900L)
        extractor.extract(6L) >> Optional.of(1000L)
        
        expect:
        searcher.search(Range.closed(0L, 10L), 1000L) == 6L
    }
    
    def "should find offset nearest to given timestamp"() {
        given:
        extractor.extract(4L) >> Optional.of(800L)
        extractor.extract(7L) >> Optional.of(1100L)
        extractor.extract(5L) >> Optional.of(999L)
        extractor.extract(6L) >> Optional.of(1001L)
        
        expect:
        searcher.search(Range.closed(0L, 10L), 1000L) == 6L
    }

    def "should find offset when event has no timestamp"() {
        given:
        extractor.extract(4L) >> Optional.empty()
        extractor.extract(7L) >> Optional.of(1100L)
        extractor.extract(5L) >> Optional.of(999L)
        extractor.extract(6L) >> Optional.of(1001L)

        expect:
        searcher.search(Range.closed(0L, 10L), 1000L) == 6L
    }
}
