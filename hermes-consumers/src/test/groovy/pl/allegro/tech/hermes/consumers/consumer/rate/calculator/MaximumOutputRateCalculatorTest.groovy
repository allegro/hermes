package pl.allegro.tech.hermes.consumers.consumer.rate.calculator

import pl.allegro.tech.hermes.api.Subscription
import pl.allegro.tech.hermes.api.SubscriptionPolicy
import pl.allegro.tech.hermes.consumers.consumer.ActiveConsumerCounter
import spock.lang.Specification

import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription

class MaximumOutputRateCalculatorTest extends Specification {

    private ActiveConsumerCounter consumerCounter = Stub(ActiveConsumerCounter)

    private MaximumOutputRateCalculator calculator = new MaximumOutputRateCalculator(consumerCounter)

    def "should calulcate maximum consumer rate as part of overall subscription rate limit"() {
        given:
        Subscription subscription = subscription("group.topic", "subscription")
                .withSubscriptionPolicy(
                SubscriptionPolicy.Builder.subscriptionPolicy()
                        .applyDefaults().withRate(1000).build()
        ).build()
        consumerCounter.countActiveConsumers(subscription) >> 4

        expect:
        calculator.calculateMaximumOutputRate(subscription) == 250D
    }
}
