package pl.allegro.tech.hermes.management.infrastructure.time

import spock.lang.Specification

import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId

class TimeFormatterTest extends Specification {

    private TimeFormatter timeFormatter = new TimeFormatter(Clock.systemDefaultZone())

    def "should parse formatted time to millis"() {
        expect:
            Math.abs(timeFormatter.parse(formattedTime) - millis) < 1000

        where:
            formattedTime           | millis
            "-7h"                   | LocalDateTime.now().minusHours(7).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            "2015-05-12T12:35:05"   | LocalDateTime.of(2015, 5, 12, 12, 35, 5).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    def "should throw exception on invalid time format"() {
        when:
            timeFormatter.parse("12312cdscsdf234")

        then:
            thrown(IllegalArgumentException)
    }

}
