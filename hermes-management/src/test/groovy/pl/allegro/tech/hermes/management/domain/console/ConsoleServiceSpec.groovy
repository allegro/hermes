package pl.allegro.tech.hermes.management.domain.console

import org.springframework.web.client.RestTemplate
import pl.allegro.tech.hermes.management.config.console.ConsoleConfigProperties
import pl.allegro.tech.hermes.management.infrastructure.console.ClasspathFileConsoleConfigurationRepository
import pl.allegro.tech.hermes.management.infrastructure.console.HttpConsoleConfigurationRepository
import spock.lang.Specification

class ConsoleServiceSpec extends Specification {

    def "Should get Hermes Console configuration from classpath resource"() {
        given:
        def properties = new ConsoleConfigProperties(location: 'console/config-test')
        def repository = new ClasspathFileConsoleConfigurationRepository(properties)
        def service = new ConsoleService(repository)

        expect:
        service.configuration == 'var config = { "property": "value" }\n'
    }

    def "Should get Hermes Console configuration from HTTP resource"() {
        given:
        def properties = new ConsoleConfigProperties(location: 'http://resource.com')
        def restTemplate = Mock(RestTemplate)
        restTemplate.getForObject('http://resource.com', String) >> '{ "property": "value" }'
        def repository = new HttpConsoleConfigurationRepository(properties, restTemplate)
        def service = new ConsoleService(repository)

        expect:
        service.configuration == 'var config = { "property": "value" }'
    }
}
