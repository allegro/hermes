package pl.allegro.tech.hermes.management.domain.maintainer.validator

import pl.allegro.tech.hermes.api.Maintainer
import pl.allegro.tech.hermes.api.MaintainerDescriptor
import pl.allegro.tech.hermes.management.domain.maintainer.MaintainerSource
import pl.allegro.tech.hermes.management.domain.maintainer.MaintainerSources
import spock.lang.Specification

class MaintainerDescriptorValidatorSpec extends Specification {

    def maintainerSources = new MaintainerSources([new FakeMaintainerSource()])
    def validator = new MaintainerDescriptorValidator(maintainerSources)

    def "should pass when maintainer exists"() {
        when:
        validator.check(new MaintainerDescriptor("fake", "Some Team"))

        then:
        noExceptionThrown()
    }

    def "should fail when maintainer source doesn't exist"() {
        when:
        validator.check(new MaintainerDescriptor("non-existing", "Some Team"))

        then:
        thrown MaintainerDescriptorValidationException
    }

    def "should fail when maintainer doesn't exist"() {
        when:
        validator.check(new MaintainerDescriptor("fake", "non-existing"))

        then:
        thrown MaintainerDescriptorValidationException
    }

    class FakeMaintainerSource implements MaintainerSource {

        @Override
        String name() {
            return 'fake'
        }

        @Override
        boolean exists(String maintainerId) {
            return maintainerId == 'Some Team'
        }

        @Override
        Maintainer get(String id) {
            return new Maintainer(id, id)
        }

        @Override
        List<Maintainer> maintainersMatching(String searchString) {
            if ('Some Team'.contains(searchString)) {
                return [new Maintainer('id-some-team', 'Some Team')]
            }
            return []
        }
    }

}
