package pl.allegro.tech.hermes.management.domain.owner.validator

import pl.allegro.tech.hermes.api.Owner
import pl.allegro.tech.hermes.api.OwnerId
import pl.allegro.tech.hermes.management.domain.owner.OwnerSource
import pl.allegro.tech.hermes.management.domain.owner.OwnerSources
import spock.lang.Specification

class OwnerIdValidatorSpec extends Specification {

    def ownerSources = new OwnerSources([new FakeOwnerSource()])
    def validator = new OwnerIdValidator(ownerSources)

    def "should pass when owner exists"() {
        when:
        validator.check(new OwnerId("fake", "Some Team"))

        then:
        noExceptionThrown()
    }

    def "should fail when owner source doesn't exist"() {
        when:
        validator.check(new OwnerId("non-existing", "Some Team"))

        then:
        thrown OwnerIdValidationException
    }

    def "should fail when owner doesn't exist"() {
        when:
        validator.check(new OwnerId("fake", "non-existing"))

        then:
        thrown OwnerIdValidationException
    }

    class FakeOwnerSource implements OwnerSource {

        @Override
        String name() {
            return 'fake'
        }

        @Override
        boolean exists(String ownerId) {
            return ownerId == 'Some Team'
        }

        @Override
        Owner get(String id) {
            return new Owner(id, id)
        }
    }

}
