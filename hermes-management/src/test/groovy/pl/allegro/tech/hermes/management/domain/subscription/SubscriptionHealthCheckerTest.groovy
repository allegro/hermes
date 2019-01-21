package pl.allegro.tech.hermes.management.domain.subscription

import pl.allegro.tech.hermes.api.Subscription
import pl.allegro.tech.hermes.api.SubscriptionHealth
import pl.allegro.tech.hermes.api.SubscriptionMetrics
import pl.allegro.tech.hermes.api.TopicMetrics
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionHealthChecker
import pl.allegro.tech.hermes.management.domain.subscription.health.problem.LaggingIndicator
import pl.allegro.tech.hermes.management.domain.subscription.health.problem.MalfunctioningIndicator
import pl.allegro.tech.hermes.management.domain.subscription.health.problem.ReceivingMalformedMessagesIndicator
import pl.allegro.tech.hermes.management.domain.subscription.health.problem.TimingOutIndicator
import pl.allegro.tech.hermes.management.domain.subscription.health.problem.UnreachableIndicator
import spock.lang.Specification
import spock.lang.Subject

import static java.lang.Integer.MAX_VALUE
import static pl.allegro.tech.hermes.api.BatchSubscriptionPolicy.Builder.batchSubscriptionPolicy
import static pl.allegro.tech.hermes.api.Subscription.State.ACTIVE
import static pl.allegro.tech.hermes.api.Subscription.State.SUSPENDED
import static pl.allegro.tech.hermes.api.SubscriptionHealth.HEALTHY
import static pl.allegro.tech.hermes.api.SubscriptionHealth.NO_DATA
import static pl.allegro.tech.hermes.api.SubscriptionHealth.Status.UNHEALTHY
import static pl.allegro.tech.hermes.api.SubscriptionHealthProblem.lagging
import static pl.allegro.tech.hermes.api.SubscriptionHealthProblem.malfunctioning
import static pl.allegro.tech.hermes.api.SubscriptionHealthProblem.receivingMalformedMessages
import static pl.allegro.tech.hermes.api.SubscriptionHealthProblem.timingOut
import static pl.allegro.tech.hermes.api.SubscriptionHealthProblem.unreachable
import static pl.allegro.tech.hermes.api.SubscriptionMetrics.Builder.subscriptionMetrics
import static pl.allegro.tech.hermes.api.SubscriptionPolicy.Builder.subscriptionPolicy
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription

class SubscriptionHealthCheckerTest extends Specification {
    static final MIN_SUBSCRIPTION_RATE_FOR_RELIABLE_METRICS = 2.0
    static final ACTIVE_SERIAL_SUBSCRIPTION = subscription("group.topic", "subscription")
            .withState(ACTIVE)
            .build()

    @Subject
    def healthChecker = new SubscriptionHealthChecker([
            new LaggingIndicator(600),
            new UnreachableIndicator(0.5, MIN_SUBSCRIPTION_RATE_FOR_RELIABLE_METRICS),
            new TimingOutIndicator(0.1, MIN_SUBSCRIPTION_RATE_FOR_RELIABLE_METRICS),
            new MalfunctioningIndicator(0.1, MIN_SUBSCRIPTION_RATE_FOR_RELIABLE_METRICS),
            new ReceivingMalformedMessagesIndicator(0.1, MIN_SUBSCRIPTION_RATE_FOR_RELIABLE_METRICS)
    ] as Set)

    def "should return healthy status for a reasonably healthy subscription"() {
        given:
        def topicMetrics = topicMetricsWithRate("123.45")
        def subscriptionMetrics = subscriptionMetrics()
                .withRate("123.45")
                .withCodes2xx("100.0")
                .withCodes4xx("1.2345")
                .withCodes5xx("1.2345")
                .withTimeouts("1.2345")
                .withOtherErrors("1.2345")
                .withLag(789)
                .withBatchRate("0.0")
                .build()

        when:
        SubscriptionHealth health = healthChecker.checkHealth(ACTIVE_SERIAL_SUBSCRIPTION, topicMetrics, subscriptionMetrics)

        then:
        health == HEALTHY
    }

    def "should indicate that a subscriber with a lag longer than 10 minutes is lagging"() {
        given:
        def topicMetrics = topicMetricsWithRate("100.0")
        def subscriptionMetrics = otherwiseHealthySubscriptionMetrics()
                .withLag(60100)
                .build()

        when:
        SubscriptionHealth health = healthChecker.checkHealth(ACTIVE_SERIAL_SUBSCRIPTION, topicMetrics, subscriptionMetrics)

        then:
        health.status == UNHEALTHY
        health.problems == [lagging(60100)] as Set
    }

    def "should not indicate lagging if production rate is 0"() {
        given:
        def topicMetrics = topicMetricsWithRate("0.0")
        def subscriptionMetrics = otherwiseHealthySubscriptionMetrics()
                .withLag(60100)
                .build()

        when:
        SubscriptionHealth health = healthChecker.checkHealth(ACTIVE_SERIAL_SUBSCRIPTION, topicMetrics, subscriptionMetrics)

        then:
        health == HEALTHY
    }

    def "should indicate that a serial subscriber with more than 50% 'other' errors is unreachable"() {
        given:
        def topicMetrics = topicMetricsWithRate("100.0")
        def subscriptionMetrics = otherwiseHealthySubscriptionMetrics()
                .withRate("100.0")
                .withOtherErrors("51.0")
                .build()

        when:
        SubscriptionHealth health = healthChecker.checkHealth(ACTIVE_SERIAL_SUBSCRIPTION, topicMetrics, subscriptionMetrics)

        then:
        health.status == UNHEALTHY
        health.problems == [unreachable(51)] as Set
    }

    def "should indicate that a batch subscriber with more than 50% 'other' errors is unreachable"() {
        given:
        def topicMetrics = topicMetricsWithRate("100.0")
        def subscriptionMetrics = otherwiseHealthySubscriptionMetrics()
                .withRate("40.0")
                .withBatchRate("4.0")
                .withOtherErrors("6.0")
                .build()

        when:
        SubscriptionHealth health = healthChecker.checkHealth(batchSubscriptionWithSize(10), topicMetrics, subscriptionMetrics)

        then:
        health.status == UNHEALTHY
        health.problems == [unreachable(6.0)] as Set
    }

    def "should indicate that a serial subscriber with more than 10% timeouts is timing out"() {
        given:
        def topicMetrics = topicMetricsWithRate("100.0")
        def subscriptionMetrics = otherwiseHealthySubscriptionMetrics()
                .withRate("100.0")
                .withTimeouts("11.0")
                .build()

        when:
        SubscriptionHealth health = healthChecker.checkHealth(ACTIVE_SERIAL_SUBSCRIPTION, topicMetrics, subscriptionMetrics)

        then:
        health.status == UNHEALTHY
        health.problems == [timingOut(11)] as Set
    }

    def "should indicate that a batch subscriber with more than 10% timeouts is timing out"() {
        given:
        def topicMetrics = topicMetricsWithRate("100.0")
        def subscriptionMetrics = otherwiseHealthySubscriptionMetrics()
                .withRate("80.0")
                .withBatchRate("8.0")
                .withTimeouts("2.0")
                .build()

        when:
        SubscriptionHealth health = healthChecker.checkHealth(batchSubscriptionWithSize(10), topicMetrics, subscriptionMetrics)

        then:
        health.status == UNHEALTHY
        health.problems == [timingOut(2.0)] as Set
    }

    def "should indicate that a serial subscriber returning more than 10% 5xx errors is malfunctioning"() {
        given:
        def topicMetrics = topicMetricsWithRate("100.0")
        def subscriptionMetrics = otherwiseHealthySubscriptionMetrics()
                .withRate("100.0")
                .withCodes5xx("11.0")
                .build()

        when:
        SubscriptionHealth health = healthChecker.checkHealth(ACTIVE_SERIAL_SUBSCRIPTION, topicMetrics, subscriptionMetrics)

        then:
        health.status == UNHEALTHY
        health.problems == [malfunctioning(11)] as Set
    }

    def "should indicate that a batch subscriber returning more than 10% 5xx errors is malfunctioning"() {
        given:
        def topicMetrics = topicMetricsWithRate("100.0")
        def subscriptionMetrics = otherwiseHealthySubscriptionMetrics()
                .withRate("80.0")
                .withBatchRate("8.0")
                .withCodes5xx("2.0")
                .build()

        when:
        SubscriptionHealth health = healthChecker.checkHealth(batchSubscriptionWithSize(10), topicMetrics, subscriptionMetrics)

        then:
        health.status == UNHEALTHY
        health.problems == [malfunctioning(2.0)] as Set
    }

    def "should indicate that a serial subscriber with retry returning more than 10% 4xx errors is receiving malformed events"() {
        given:
        def retrySubscriptionPolicy = subscriptionPolicy()
                .withClientErrorRetry()
                .build()
        def subscriptionWithRetry = subscription("group.topic", "subscription")
                .withSubscriptionPolicy(retrySubscriptionPolicy)
                .build()
        def topicMetrics = topicMetricsWithRate("100.0")
        def subscriptionMetrics = otherwiseHealthySubscriptionMetrics()
                .withRate("100.0")
                .withCodes4xx("11.0")
                .build()

        when:
        SubscriptionHealth health = healthChecker.checkHealth(subscriptionWithRetry, topicMetrics, subscriptionMetrics)

        then:
        health.status == UNHEALTHY
        health.problems == [receivingMalformedMessages(11.0)] as Set
    }

    def "should indicate that a batch subscriber with retry returning more than 10% 4xx errors is receiving malformed events"() {
        given:
        def retrySubscriptionPolicy = batchSubscriptionPolicy()
                .withClientErrorRetry(true)
                .build()
        def subscriptionWithRetry = subscription("group.topic", "subscription")
                .withSubscriptionPolicy(retrySubscriptionPolicy)
                .build()
        def topicMetrics = topicMetricsWithRate("100.0")
        def subscriptionMetrics = otherwiseHealthySubscriptionMetrics()
                .withRate("100.0")
                .withBatchRate("8.0")
                .withCodes4xx("2.0")
                .build()

        when:
        SubscriptionHealth health = healthChecker.checkHealth(subscriptionWithRetry, topicMetrics, subscriptionMetrics)

        then:
        health.status == UNHEALTHY
        health.problems == [receivingMalformedMessages(2.0)] as Set
    }

    def "should not indicate that a serial subscriber without retry returning more than 10% 4xx errors is receiving malformed events"() {
        given:
        def subscriptionWithoutRetry = ACTIVE_SERIAL_SUBSCRIPTION
        def topicMetrics = topicMetricsWithRate("100.0")
        def subscriptionMetrics = otherwiseHealthySubscriptionMetrics()
                .withRate("100.0")
                .withCodes4xx("11.0")
                .build()

        when:
        SubscriptionHealth health = healthChecker.checkHealth(subscriptionWithoutRetry, topicMetrics, subscriptionMetrics)

        then:
        health == HEALTHY
    }

    def "should not indicate that a batch subscriber without retry returning more than 10% 4xx errors is receiving malformed events"() {
        given:
        def noRetrySubscriptionPolicy = batchSubscriptionPolicy()
                .withClientErrorRetry(false)
                .build()
        def subscriptionWithoutRetry = subscription("group.topic", "subscription")
                .withSubscriptionPolicy(noRetrySubscriptionPolicy)
                .build()
        def topicMetrics = topicMetricsWithRate("100.0")
        def subscriptionMetrics = otherwiseHealthySubscriptionMetrics()
                .withRate("100.0")
                .withBatchRate("8.0")
                .withCodes4xx("2.0")
                .build()

        when:
        SubscriptionHealth health = healthChecker.checkHealth(subscriptionWithoutRetry, topicMetrics, subscriptionMetrics)

        then:
        health == HEALTHY
    }

    def "should return healthy status for a suspended subscription even if its metrics are not healthy"() {
        given:
        def suspendedSubscription = subscription("group.topic", "subscription")
                .withState(SUSPENDED)
                .build()
        def topicMetrics = topicMetricsWithRate("1000.0")
        def subscriptionMetrics = subscriptionMetrics()
                .withRate("160.0")
                .withCodes2xx("0.0")
                .withCodes4xx("20.0")
                .withCodes5xx("20.0")
                .withTimeouts("20.0")
                .withOtherErrors("100.0")
                .withLag(1_000_000)
                .build()

        when:
        SubscriptionHealth health = healthChecker.checkHealth(suspendedSubscription, topicMetrics, subscriptionMetrics)

        then:
        health == HEALTHY
    }

    def "should return healthy status for a healthy subscription when the topic is idle"() {
        given:
        def topicMetrics = topicMetricsWithRate("0.0")
        def subscriptionMetrics = otherwiseHealthySubscriptionMetrics().build()

        when:
        SubscriptionHealth health = healthChecker.checkHealth(ACTIVE_SERIAL_SUBSCRIPTION, topicMetrics, subscriptionMetrics)

        then:
        health == HEALTHY
    }

    def "should return 'no data' status and no problems when some of the metrics are unavailable even if others can be calculated"() {
        given:
        def topicMetrics = topicMetricsWithRate("100.0")
        def subscriptionMetrics = otherwiseHealthySubscriptionMetrics()
                .withRate("unavailable")
                .withLag(60100)
                .build()

        when:
        SubscriptionHealth health = healthChecker.checkHealth(ACTIVE_SERIAL_SUBSCRIPTION, topicMetrics, subscriptionMetrics)

        then:
        health == NO_DATA
    }

    static TopicMetrics topicMetricsWithRate(String rate) {
        TopicMetrics.Builder.topicMetrics()
                .withRate(rate)
                .build()
    }

    static SubscriptionMetrics.Builder otherwiseHealthySubscriptionMetrics() {
        subscriptionMetrics()
                .withRate("100.0")
                .withCodes2xx("100.0")
                .withCodes4xx("0.0")
                .withCodes5xx("0.0")
                .withTimeouts("0.0")
                .withOtherErrors("0.0")
                .withLag(0)
                .withBatchRate("0.0")
    }

    private static Subscription batchSubscriptionWithSize(int batchSize) {
        subscription("group.topic", "subscription")
                .withState(ACTIVE)
                .withSubscriptionPolicy(batchSubscriptionPolicy()
                    .withMessageTtl(100)
                    .withRequestTimeout(100)
                    .withMessageBackoff(10)
                    .withBatchSize(batchSize)
                    .withBatchTime(MAX_VALUE)
                    .withBatchVolume(1024)
                    .build())
                .build()
    }
}
