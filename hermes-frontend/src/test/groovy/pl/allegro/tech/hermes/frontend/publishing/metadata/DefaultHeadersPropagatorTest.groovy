package pl.allegro.tech.hermes.frontend.publishing.metadata

import pl.allegro.tech.hermes.common.config.ConfigFactory
import spock.lang.Specification

import static pl.allegro.tech.hermes.common.config.Configs.FRONTEND_HEADER_PROPAGATION_ENABLED;
import static pl.allegro.tech.hermes.common.config.Configs.FRONTEND_HEADER_PROPAGATION_ALLOW_FILTER;

class DefaultHeadersPropagatorTest extends Specification {

    def 'should propagate all headers when enabled and no filter'() {
        given:
            def configFactory = Mock(ConfigFactory) {
                getBooleanProperty(FRONTEND_HEADER_PROPAGATION_ENABLED) >> true
                getStringProperty(FRONTEND_HEADER_PROPAGATION_ALLOW_FILTER) >> ""
            }
            def propagator = new DefaultHeadersPropagator(configFactory);

        when:
            def extracted = propagator.extract(["header1": "value1", "header2": "value2"])

        then:
            extracted.size() == 2
            extracted["header1"] == "value1"
            extracted["header2"] == "value2"
    }

    def 'should not propagate headers when disabled'() {
        given:
            def configFactory = Mock(ConfigFactory) {
                getBooleanProperty(FRONTEND_HEADER_PROPAGATION_ENABLED) >> false
            }
            def propagator = new DefaultHeadersPropagator(configFactory);

        when:
            def extracted = propagator.extract(["header1": "value1", "header2": "value2"])

        then:
            extracted.size() == 0
    }

    def 'should propagate only filtered headers when enabled and filter defined'() {
        given:
            def configFactory = Mock(ConfigFactory) {
                getBooleanProperty(FRONTEND_HEADER_PROPAGATION_ENABLED) >> true
                getStringProperty(FRONTEND_HEADER_PROPAGATION_ALLOW_FILTER) >> "other-header, header1"
            }
            def propagator = new DefaultHeadersPropagator(configFactory);

        when:
            def extracted = propagator.extract(["header1": "value1", "header2": "value2"])

        then:
            extracted.size() == 1
            extracted["header1"] == "value1"
    }
}