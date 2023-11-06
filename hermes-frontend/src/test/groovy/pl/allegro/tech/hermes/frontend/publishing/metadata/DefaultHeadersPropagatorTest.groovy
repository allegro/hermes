package pl.allegro.tech.hermes.frontend.publishing.metadata

import io.undertow.util.HeaderMap
import io.undertow.util.HttpString
import pl.allegro.tech.hermes.frontend.config.HTTPHeadersProperties
import spock.lang.Specification

class DefaultHeadersPropagatorTest extends Specification {

    def 'should propagate all headers when enabled and no filter'() {
        given:
            def properties = new HTTPHeadersProperties()
            properties.setPropagationEnabled(true)
            def propagator = new DefaultHeadersPropagator(properties)

            HeaderMap headerMap = new HeaderMap()
            headerMap.add(new HttpString("header1"), "value1")
            headerMap.add(new HttpString("header2"), "value2")
        when:
            def extracted = propagator.extract(headerMap)

        then:
            extracted.size() == 2
            extracted["header1"] == "value1"
            extracted["header2"] == "value2"
    }

    def 'should not propagate headers when propagation is disabled by default'() {
        given:
            def propagator = new DefaultHeadersPropagator(new HTTPHeadersProperties())
            HeaderMap headerMap = new HeaderMap()
            headerMap.add(new HttpString("header1"), "value1")
        when:
            def extracted = propagator.extract(headerMap)

        then:
            extracted.size() == 0
    }

    def 'should propagate only allowed headers when enabled and allowed defined'() {
        given:
            def properties = new HTTPHeadersProperties()
            properties.setPropagationEnabled(true)
            properties.setAllowedSet(["header1", "other-header"] as Set)
            properties.setAdditionalAllowedSetToLog(["service-name"] as Set)
            def propagator = new DefaultHeadersPropagator(properties)
            HeaderMap headerMap = new HeaderMap()
            headerMap.add(new HttpString("header1"), "value1")
            headerMap.add(new HttpString("header2"), "value2")
            headerMap.add(new HttpString("service-name"), "value2")
        when:
            def extracted = propagator.extract(headerMap)

        then:
            extracted.size() == 1
            extracted["header1"] == "value1"
    }

    def 'should extract "log only" headers'() {
        given:
            def properties = new HTTPHeadersProperties()
            properties.setPropagationEnabled(true)
            properties.setAllowedSet(["header1", "other-header"] as Set)
            properties.setAdditionalAllowedSetToLog(["service-name"] as Set)
            def propagator = new DefaultHeadersPropagator(properties)
            HeaderMap headerMap = new HeaderMap()
            headerMap.add(new HttpString("header1"), "value1")
            headerMap.add(new HttpString("service-name"), "value2")
        when:
            def extracted = propagator.extractHeadersToLog(headerMap)

        then:
            extracted.size() == 2
            extracted["header1"] == "value1"
            extracted["service-name"] == "value2"
    }
}
