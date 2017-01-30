package pl.allegro.tech.hermes.management.domain.owner

import pl.allegro.tech.hermes.api.Owner
import pl.allegro.tech.hermes.management.infrastructure.crowd.CrowdClient
import spock.lang.Specification

class CrowdOwnerSourceSpec extends Specification {

    def client = Stub(CrowdClient) {
        getGroups("Tea") >> ["Team A", "Team B"]
        getGroups("None") >> []
    }

    def source = new CrowdOwnerSource(client)

    def "should search matching owners"() {
        expect:
        source.ownersMatching("Tea") == [new Owner("Team A", "Team A"), new Owner("Team B", "Team B")]
    }

    def "should search matching owners with already matched groups"() {
        expect:
        source.ownersMatching(searchString) == [
                new Owner("Team A, Team A", "Team A, Team A"),
                new Owner("Team A, Team B", "Team A, Team B")
        ]
        where:
        searchString << ["Team A, Tea", "Team A,Tea", " Team A, Tea "]
    }

}
