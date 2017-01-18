package pl.allegro.tech.hermes.management.domain.maintainer

import pl.allegro.tech.hermes.api.Maintainer
import pl.allegro.tech.hermes.management.infrastructure.crowd.CrowdClient
import spock.lang.Specification

class CrowdMaintainerSourceSpec extends Specification {

    def client = Stub(CrowdClient) {
        getGroups("Tea") >> ["Team A", "Team B"]
        getGroups("None") >> []
    }

    def source = new CrowdMaintainerSource(client)

    def "should search matching maintainers"() {
        expect:
        source.maintainersMatching("Tea") == [new Maintainer("Team A", "Team A"), new Maintainer("Team B", "Team B")]
    }

    def "should search matching maintainers with already matched groups"() {
        expect:
        source.maintainersMatching(searchString) == [
                new Maintainer("Team A, Team A", "Team A, Team A"),
                new Maintainer("Team A, Team B", "Team A, Team B")
        ]
        where:
        searchString << ["Team A, Tea", "Team A,Tea", " Team A, Tea "]
    }

}
