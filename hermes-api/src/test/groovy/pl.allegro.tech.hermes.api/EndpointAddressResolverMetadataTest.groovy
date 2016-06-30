package pl.allegro.tech.hermes.api

import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Specification

import static pl.allegro.tech.hermes.api.EndpointAddressResolverMetadata.endpointAddressResolverMetadata

class EndpointAddressResolverMetadataTest extends Specification {

    def "should deserialize from and to map"() {
        given:
        def sourceJsonList = '{"A":"something","B":123}';
        def objectMapper = new ObjectMapper();

        when:
        def metadata = objectMapper.readValue(sourceJsonList, EndpointAddressResolverMetadata.class)

        then:
        assert metadata.get('A').get() == 'something'
        assert metadata.get('B').get() == 123

        when:
        def serializedMetadata = objectMapper.writeValueAsString(metadata)

        then:
        assert sourceJsonList == serializedMetadata
    }

    def "should return default value if not present"() {
        when:
        def metadata = endpointAddressResolverMetadata()
                .withEntry("A", "something")
                .build()

        then:
        metadata.getOrDefault("A", "default") == "something"
        metadata.getOrDefault("B", 123) == 123
    }

    def "should create metadata object using provided builder class"() {
        when:
        def metadata = endpointAddressResolverMetadata()
                .withEntry("A", "something")
                .withEntry("B", 123)
                .build()

        then:
        metadata.entries.size() == 2
        metadata.get("A").get() == "something"
        metadata.get("B").get() == 123
        !metadata.get("C").isPresent()
    }
}
