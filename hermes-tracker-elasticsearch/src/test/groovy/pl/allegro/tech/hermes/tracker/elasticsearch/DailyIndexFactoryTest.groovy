package pl.allegro.tech.hermes.tracker.elasticsearch
import pl.allegro.tech.hermes.tracker.elasticsearch.consumers.ConsumersDailyIndexFactory
import pl.allegro.tech.hermes.tracker.elasticsearch.frontend.FrontendDailyIndexFactory
import spock.lang.Shared
import spock.lang.Specification

import java.time.Clock

import static java.time.LocalDate.of
import static java.time.ZoneId.systemDefault
import static java.time.ZoneOffset.UTC

class DailyIndexFactoryTest extends Specification {

    @Shared
    def clock = Clock.fixed(of(2000, 1, 1).atStartOfDay().toInstant(UTC), systemDefault())

    def "should create daily index"() {
        expect:
        indexFactory.createIndex().endsWith("_2000_01_01")

        where:
        indexFactory << [new FrontendDailyIndexFactory(clock), new ConsumersDailyIndexFactory(clock)]
    }
}