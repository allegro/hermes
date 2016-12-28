package pl.allegro.tech.hermes.management.domain.subscription.validator

import pl.allegro.tech.hermes.api.Maintainer
import pl.allegro.tech.hermes.management.domain.maintainer.MaintainerSource
import pl.allegro.tech.hermes.management.domain.maintainer.MaintainerSources
import spock.lang.Specification

import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription

class SubscriptionValidatorSpec extends Specification {

    def maintainerSources = new MaintainerSources([new FakeMaintainerSource()])
    def validator = new SubscriptionValidator(maintainerSources)

    def "should pass when maintainer exists"() {
        given:
        def toCheck = subscription("group.topic", "sub").withMaintainer(new Maintainer("fake", "Some Team")).build()

        when:
        validator.check(toCheck)

        then:
        noExceptionThrown()
    }

    def "should fail when maintainer source doesn't exist"() {
        given:
        def toCheck = subscription("group.topic", "sub").withMaintainer(new Maintainer("non-existing", "Some Team")).build()

        when:
        validator.check(toCheck)

        then:
        thrown SubscriptionValidationException
    }

    def "should fail when maintainer doesn't exist"() {
        given:
        def toCheck = subscription("group.topic", "sub").withMaintainer(new Maintainer("fake", "non-existing")).build()

        when:
        validator.check(toCheck)

        then:
        thrown SubscriptionValidationException
    }

    class FakeMaintainerSource implements MaintainerSource {

        @Override
        String sourceName() {
            return 'fake'
        }

        @Override
        boolean exists(String maintainerId) {
            return maintainerId == 'Some Team'
        }

        @Override
        List<String> maintainersMatching(String searchString) {
            if ('Some Team'.contains(searchString)) {
                return ['Some Team']
            }
            return []
        }
    }

}
