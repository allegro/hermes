package pl.allegro.tech.hermes.frontend.publishing.metadata

import spock.lang.Specification

class DefaultHeadersPropagatorTest extends Specification {

    def 'should propagate all headers when enabled and no filter'() {
        given:
            def propagator = new DefaultHeadersPropagator(true, "")

        when:
            def extracted = propagator.extract(["header1": "value1", "header2": "value2"])

        then:
            extracted.size() == 2
            extracted["header1"] == "value1"
            extracted["header2"] == "value2"
    }

    def 'should not propagate headers when disabled'() {
        given:
            def propagator = new DefaultHeadersPropagator(false, "")

        when:
            def extracted = propagator.extract(["header1": "value1", "header2": "value2"])

        then:
            extracted.size() == 0
    }

    def 'should propagate only filtered headers when enabled and filter defined'() {
        given:
            def propagator = new DefaultHeadersPropagator(true, "other-header, header1")

        when:
            def extracted = propagator.extract(["header1": "value1", "header2": "value2"])

        then:
            extracted.size() == 1
            extracted["header1"] == "value1"
    }
}