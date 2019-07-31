package pl.allegro.tech.hermes.consumers.subscription.id

import pl.allegro.tech.hermes.api.SubscriptionName
import spock.lang.Specification

class SubscriptionIdTest extends Specification {

    def "should be same when creating both from subscription name and int value"() {
        given:
        def subscriptionName = SubscriptionName.fromString('pl.allegro.tech.some.subscription$someSub')

        when:
        def id = SubscriptionId.of(subscriptionName)

        then:
        SubscriptionId.from(id.value) == id
    }
}
