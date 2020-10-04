package pl.allegro.tech.hermes.domain.filtering

import pl.allegro.tech.hermes.api.MessageFilterSpecification
import pl.allegro.tech.hermes.domain.filtering.header.HeaderSubscriptionMessageFilterCompiler
import spock.lang.Specification
import spock.lang.Unroll

class HeaderMessageFilterSpec extends Specification {

    @Unroll
    def "should filter message by headers"() {
        given:
            def spec = new MessageFilterSpecification([header: "a", matcher: matcher])
            def msg = FilterableBuilder
                    .withTestMessage()
                    .withExternalMetadata(headers)
                    .build()

        expect:
            result == new HeaderSubscriptionMessageFilterCompiler().compile(spec).test(msg)

        where:
            headers                | matcher || result
            [a: "0001"]            | "0001"  || true
            [a: "0001"]            | ".*"    || true
            [a: "0001", b: "0002"] | "0001"  || true
            [a: "0001", b: "0002"] | "0002"  || false
            [b: "0001"]            | ".*"    || false
            [:]                    | ".*"    || false
    }
}


