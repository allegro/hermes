package pl.allegro.tech.hermes.management.infrastructure.kafka.service.retransmit

import com.google.common.collect.Range
import spock.lang.Specification

class OffsetSearcherTest extends Specification {

    private KafkaTimestampExtractor extractor = Stub(KafkaTimestampExtractor)
    
    private OffsetSearcher searcher = new OffsetSearcher(extractor)

    def "should find offset for given timestamp"() {
        given:
        extractor.extract(4L) >> 800L
        extractor.extract(7L) >> 1100L
        extractor.extract(5L) >> 900L
        extractor.extract(6L) >> 1000L
        
        expect:
        searcher.search(Range.closed(0L, 10L), 1000L) == 6L
    }
    
    def "should find offset nearest to given timestamp"() {
        given:
        extractor.extract(4L) >> 800L
        extractor.extract(7L) >> 1100L
        extractor.extract(5L) >> 999L
        extractor.extract(6L) >> 1001L
        
        expect:
        searcher.search(Range.closed(0L, 10L), 1000L) == 6L
    }

}
