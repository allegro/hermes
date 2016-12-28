package pl.allegro.tech.hermes.management.domain.maintainer

import spock.lang.Specification

class MaintainerSourcesSpec extends Specification {

    def "should not allow creating with no sources configured"() {
        when:
        new MaintainerSources([])

        then:
        thrown IllegalArgumentException
    }

}
