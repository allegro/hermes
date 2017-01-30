package pl.allegro.tech.hermes.management.domain.owner

import spock.lang.Specification

class OwnerSourcesSpec extends Specification {

    def "should not allow creating with no sources configured"() {
        when:
        new OwnerSources([])

        then:
        thrown IllegalArgumentException
    }

}
