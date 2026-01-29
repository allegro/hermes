package pl.allegro.tech.hermes.management.domain.subscription

import pl.allegro.tech.hermes.api.MetricLongValue
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
import static pl.allegro.tech.hermes.api.MetricDecimalValue.of
import static pl.allegro.tech.hermes.api.MetricDecimalValue.unavailable
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
    ])

    def "should return healthy status for a reasonably healthy subscription"() {
        given:
        def topicMetrics = topicMetricsWithRate("123.45")
        def subscriptionMetrics = subscriptionMetrics()
                .withRate(of("123.45"))
                .withCodes2xx(of("100.0"))
                .withCodes4xx(of("1.2345"))
                .withCodes5xx(of("1.2345"))
                .withTimeouts(of("1.2345"))
                .withOtherErrors(of("1.2345"))
                .withLag(MetricLongValue.of(789))
                .withBatchRate(of("0.0"))
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
                .withLag(MetricLongValue.of(60100))
                .build()

        when:
        SubscriptionHealth health = healthChecker.checkHealth(ACTIVE_SERIAL_SUBSCRIPTION, topicMetrics, subscriptionMetrics)

        then:
        health.status == UNHEALTHY
        health.problems == [lagging(60100, ACTIVE_SERIAL_SUBSCRIPTION.getQualifiedName().toString())] as Set
    }

    def "should not indicate lagging if production rate is 0"() {
        given:
        def topicMetrics = topicMetricsWithRate("0.0")
        def subscriptionMetrics = otherwiseHealthySubscriptionMetrics()
                .withLag(MetricLongValue.of(60100))
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
                .withRate(of("100.0"))
                .withOtherErrors(of("51.0"))
                .build()

        when:
        SubscriptionHealth health = healthChecker.checkHealth(ACTIVE_SERIAL_SUBSCRIPTION, topicMetrics, subscriptionMetrics)

        then:
        health.status == UNHEALTHY
        health.problems == [unreachable(51, ACTIVE_SERIAL_SUBSCRIPTION.getQualifiedName().toString())] as Set
    }

    def "should indicate that a batch subscriber with more than 50% 'other' errors is unreachable"() {
        given:
        def topicMetrics = topicMetricsWithRate("100.0")
        def subscriptionMetrics = otherwiseHealthySubscriptionMetrics()
                .withRate(of("40.0"))
                .withBatchRate(of("4.0"))
                .withOtherErrors(of("6.0"))
                .build()
        def batchSubscription = batchSubscriptionWithSize(10)

        when:
        SubscriptionHealth health = healthChecker.checkHealth(batchSubscription, topicMetrics, subscriptionMetrics)

        then:
        health.status == UNHEALTHY
        health.problems == [unreachable(6.0, batchSubscription.getQualifiedName().toString())] as Set
    }

    def "should indicate that a serial subscriber with more than 10% timeouts is timing out"() {
        given:
        def topicMetrics = topicMetricsWithRate("100.0")
        def subscriptionMetrics = otherwiseHealthySubscriptionMetrics()
                .withRate(of("100.0"))
                .withTimeouts(of("11.0"))
                .build()

        when:
        SubscriptionHealth health = healthChecker.checkHealth(ACTIVE_SERIAL_SUBSCRIPTION, topicMetrics, subscriptionMetrics)

        then:
        health.status == UNHEALTHY
        health.problems == [timingOut(11, ACTIVE_SERIAL_SUBSCRIPTION.getQualifiedName().toString())] as Set
    }

    def "should indicate that a batch subscriber with more than 10% timeouts is timing out"() {
        given:
        def topicMetrics = topicMetricsWithRate("100.0")
        def subscriptionMetrics = otherwiseHealthySubscriptionMetrics()
                .withRate(of("80.0"))
                .withBatchRate(of("8.0"))
                .withTimeouts(of("2.0"))
                .build()
        def batchSubscription = batchSubscriptionWithSize(10)

        when:
        SubscriptionHealth health = healthChecker.checkHealth(batchSubscription, topicMetrics, subscriptionMetrics)

        then:
        health.status == UNHEALTHY
        health.problems == [timingOut(2.0, batchSubscription.getQualifiedName().toString())] as Set
    }

    def "should indicate that a serial subscriber returning more than 10% 5xx errors is malfunctioning"() {
        given:
        def topicMetrics = topicMetricsWithRate("100.0")
        def subscriptionMetrics = otherwiseHealthySubscriptionMetrics()
                .withRate(of("100.0"))
                .withCodes5xx(of("11.0"))
                .build()

        when:
        SubscriptionHealth health = healthChecker.checkHealth(ACTIVE_SERIAL_SUBSCRIPTION, topicMetrics, subscriptionMetrics)

        then:
        health.status == UNHEALTHY
        health.problems == [malfunctioning(11, ACTIVE_SERIAL_SUBSCRIPTION.getQualifiedName().toString())] as Set
    }

    def "should indicate that a batch subscriber returning more than 10% 5xx errors is malfunctioning"() {
        given:
        def topicMetrics = topicMetricsWithRate("100.0")
        def subscriptionMetrics = otherwiseHealthySubscriptionMetrics()
                .withRate(of("80.0"))
                .withBatchRate(of("8.0"))
                .withCodes5xx(of("2.0"))
                .build()
        def batchSubscription = batchSubscriptionWithSize(10)

        when:
        SubscriptionHealth health = healthChecker.checkHealth(batchSubscription, topicMetrics, subscriptionMetrics)

        then:
        health.status == UNHEALTHY
        health.problems == [malfunctioning(2.0, batchSubscription.getQualifiedName().toString())] as Set
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
                .withRate(of("100.0"))
                .withCodes4xx(of("11.0"))
                .build()

        when:
        SubscriptionHealth health = healthChecker.checkHealth(subscriptionWithRetry, topicMetrics, subscriptionMetrics)

        then:
        health.status == UNHEALTHY
        health.problems == [receivingMalformedMessages(11.0, subscriptionWithRetry.getQualifiedName().toString())] as Set
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
                .withRate(of("100.0"))
                .withBatchRate(of("8.0"))
                .withCodes4xx(of("2.0"))
                .build()

        when:
        SubscriptionHealth health = healthChecker.checkHealth(subscriptionWithRetry, topicMetrics, subscriptionMetrics)

        then:
        health.status == UNHEALTHY
        health.problems == [receivingMalformedMessages(2.0, subscriptionWithRetry.getQualifiedName().toString())] as Set
    }

    def "should not indicate that a serial subscriber without retry returning more than 10% 4xx errors is receiving malformed events"() {
        given:
        def subscriptionWithoutRetry = ACTIVE_SERIAL_SUBSCRIPTION
        def topicMetrics = topicMetricsWithRate("100.0")
        def subscriptionMetrics = otherwiseHealthySubscriptionMetrics()
                .withRate(of("100.0"))
                .withCodes4xx(of("11.0"))
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
                .withRate(of("100.0"))
                .withBatchRate(of("8.0"))
                .withCodes4xx(of("2.0"))
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
                .withRate(of("160.0"))
                .withCodes2xx(of("0.0"))
                .withCodes4xx(of("20.0"))
                .withCodes5xx(of("20.0"))
                .withTimeouts(of("20.0"))
                .withOtherErrors(of("100.0"))
                .withLag(MetricLongValue.of(1_000_000))
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
                .withRate(unavailable())
                .withLag(MetricLongValue.of(60100))
                .build()

        when:
        SubscriptionHealth health = healthChecker.checkHealth(ACTIVE_SERIAL_SUBSCRIPTION, topicMetrics, subscriptionMetrics)

        then:
        health == NO_DATA
    }

    static TopicMetrics topicMetricsWithRate(String rate) {
        TopicMetrics.Builder.topicMetrics()
                .withRate(of(rate))
                .build()
    }

    static SubscriptionMetrics.Builder otherwiseHealthySubscriptionMetrics() {
        subscriptionMetrics()
                .withRate(of("100.0"))
                .withCodes2xx(of("100.0"))
                .withCodes4xx(of("0.0"))
                .withCodes5xx(of("0.0"))
                .withTimeouts(of("0.0"))
                .withOtherErrors(of("0.0"))
                .withLag(MetricLongValue.of(0))
                .withBatchRate(of("0.0"))
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
