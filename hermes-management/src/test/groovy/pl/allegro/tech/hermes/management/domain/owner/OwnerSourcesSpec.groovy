package pl.allegro.tech.hermes.management.domain.owner

import pl.allegro.tech.hermes.api.Owner
import spock.lang.Specification

class OwnerSourcesSpec extends Specification {

    def "should create sources"() {
        when:
        def sources = new OwnerSources([sourceNamed("a"), sourceNamed("b")])

        then:
        sources.iterator().collect { it.name() } == ["a", "b"]
    }

    def "should not allow creating with no sources configured"() {
        when:
        new OwnerSources([])

        then:
        thrown IllegalArgumentException
    }

    def "should not allow creating with duplicate source names"() {
        when:
        new OwnerSources([sourceNamed("a"), sourceNamed("a")])

        then:
        thrown IllegalArgumentException
    }

    OwnerSource sourceNamed(String name) {
        new OwnerSource() {
            @Override
            String name() {
                return name
            }

            @Override
            boolean exists(String ownerId) {
                return false
            }

            @Override
            Owner get(String id) throws OwnerSource.OwnerNotFound {
                return null
            }
        }
    }

}
