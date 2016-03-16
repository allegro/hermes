package pl.allegro.tech.hermes.management.domain.subscription

import pl.allegro.tech.hermes.api.SubscriptionHealth
import pl.allegro.tech.hermes.api.SubscriptionMetrics
import pl.allegro.tech.hermes.api.TopicMetrics
import pl.allegro.tech.hermes.management.domain.subscription.health.SubscriptionHealthChecker
import pl.allegro.tech.hermes.management.domain.subscription.health.problem.LaggingIndicator
import pl.allegro.tech.hermes.management.domain.subscription.health.problem.MalfunctioningIndicator
import pl.allegro.tech.hermes.management.domain.subscription.health.problem.ReceivingMalformedMessagesIndicator
import pl.allegro.tech.hermes.management.domain.subscription.health.problem.SlowIndicator
import pl.allegro.tech.hermes.management.domain.subscription.health.problem.TimingOutIndicator
import pl.allegro.tech.hermes.management.domain.subscription.health.problem.UnreachableIndicator
import spock.lang.Specification
import spock.lang.Subject

import static pl.allegro.tech.hermes.api.BatchSubscriptionPolicy.Builder.batchSubscriptionPolicy
import static pl.allegro.tech.hermes.api.Subscription.State.ACTIVE
import static pl.allegro.tech.hermes.api.Subscription.State.SUSPENDED
import static pl.allegro.tech.hermes.api.SubscriptionHealth.HEALTHY
import static pl.allegro.tech.hermes.api.SubscriptionHealth.NO_DATA
import static pl.allegro.tech.hermes.api.SubscriptionHealth.Problem.*
import static pl.allegro.tech.hermes.api.SubscriptionHealth.Status.UNHEALTHY
import static pl.allegro.tech.hermes.api.SubscriptionMetrics.Builder.subscriptionMetrics
import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription

class SubscriptionHealthCheckerTest extends Specification {
    static final MIN_SUBSCRIPTION_RATE_FOR_RELIABLE_METRICS = 2.0
    static final ACTIVE_SUBSCRIPTION = subscription("group.topic", "subscription")
            .withState(ACTIVE)
            .build()

    @Subject
    def subscriptionHealthChecker = new SubscriptionHealthChecker([
            new LaggingIndicator(600),
            new SlowIndicator(0.8),
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
                .build()

        when:
        SubscriptionHealth subscriptionHealth = subscriptionHealthChecker.checkHealth(ACTIVE_SUBSCRIPTION, topicMetrics, subscriptionMetrics)

        then:
        subscriptionHealth == HEALTHY
    }

    def "should indicate that a subscriber with a lag longer than 10 minutes is lagging"() {
        given:
        def topicMetrics = topicMetricsWithRate("100.0")
        def subscriptionMetrics = otherwiseHealthySubscriptionMetrics()
                .withLag(60100)
                .build()

        when:
        SubscriptionHealth subscriptionHealth = subscriptionHealthChecker.checkHealth(ACTIVE_SUBSCRIPTION, topicMetrics, subscriptionMetrics)

        then:
        subscriptionHealth.status == UNHEALTHY
        subscriptionHealth.problems == [LAGGING] as Set
    }

    def "should indicate that a subscriber whose speed is smaller than 80% of the topic speed is slow"() {
        given:
        def topicMetrics = topicMetricsWithRate("100.0")
        def subscriptionMetrics = otherwiseHealthySubscriptionMetrics()
                .withRate("79.0")
                .build()

        when:
        SubscriptionHealth subscriptionHealth = subscriptionHealthChecker.checkHealth(ACTIVE_SUBSCRIPTION, topicMetrics, subscriptionMetrics)

        then:
        subscriptionHealth.status == UNHEALTHY
        subscriptionHealth.problems == [SLOW] as Set
    }

    def "should indicate that a subscriber with more than 50% 'other' errors is unreachable"() {
        given:
        def topicMetrics = topicMetricsWithRate("100.0")
        def subscriptionMetrics = otherwiseHealthySubscriptionMetrics()
                .withRate("100.0")
                .withOtherErrors("51.0")
                .build()

        when:
        SubscriptionHealth subscriptionHealth = subscriptionHealthChecker.checkHealth(ACTIVE_SUBSCRIPTION, topicMetrics, subscriptionMetrics)

        then:
        subscriptionHealth.status == UNHEALTHY
        subscriptionHealth.problems == [UNREACHABLE] as Set
    }

    def "should indicate that a subscriber with more than 10% timeouts is timing out"() {
        given:
        def topicMetrics = topicMetricsWithRate("100.0")
        def subscriptionMetrics = otherwiseHealthySubscriptionMetrics()
                .withRate("100.0")
                .withTimeouts("11.0")
                .build()

        when:
        SubscriptionHealth subscriptionHealth = subscriptionHealthChecker.checkHealth(ACTIVE_SUBSCRIPTION, topicMetrics, subscriptionMetrics)

        then:
        subscriptionHealth.status == UNHEALTHY
        subscriptionHealth.problems == [TIMING_OUT] as Set
    }

    def "should indicate that a subscriber returning more than 10% 5xx errors is malfunctioning"() {
        given:
        def topicMetrics = topicMetricsWithRate("100.0")
        def subscriptionMetrics = otherwiseHealthySubscriptionMetrics()
                .withRate("100.0")
                .withCodes5xx("11.0")
                .build()

        when:
        SubscriptionHealth subscriptionHealth = subscriptionHealthChecker.checkHealth(ACTIVE_SUBSCRIPTION, topicMetrics, subscriptionMetrics)

        then:
        subscriptionHealth.status == UNHEALTHY
        subscriptionHealth.problems == [MALFUNCTIONING] as Set
    }

    def "should indicate that a subscriber with client error retry returning more than 10% 4xx errors is receiving malformed events"() {
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
                .withCodes4xx("11.0")
                .build()

        when:
        SubscriptionHealth subscriptionHealth = subscriptionHealthChecker.checkHealth(subscriptionWithRetry, topicMetrics, subscriptionMetrics)

        then:
        subscriptionHealth.status == UNHEALTHY
        subscriptionHealth.problems == [RECEIVING_MALFORMED_MESSAGES] as Set
    }

    def "should not indicate that a subscriber without client error retry returning more than 10% 4xx errors is receiving malformed events"() {
        given:
        def subscriptionWithoutRetry = ACTIVE_SUBSCRIPTION
        def topicMetrics = topicMetricsWithRate("100.0")
        def subscriptionMetrics = otherwiseHealthySubscriptionMetrics()
                .withRate("100.0")
                .withCodes4xx("11.0")
                .build()

        when:
        SubscriptionHealth subscriptionHealth = subscriptionHealthChecker.checkHealth(subscriptionWithoutRetry, topicMetrics, subscriptionMetrics)

        then:
        subscriptionHealth == HEALTHY
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
        SubscriptionHealth subscriptionHealth = subscriptionHealthChecker.checkHealth(suspendedSubscription, topicMetrics, subscriptionMetrics)

        then:
        subscriptionHealth == HEALTHY
    }

    def "should indicate lagging and slowness but not other health problems for a lagging and slow subscriber with rate lower than 2"() {
        given:
        def topicMetrics = topicMetricsWithRate("100.0")
        def subscriptionMetrics = subscriptionMetrics()
                .withRate("1.9")
                .withCodes2xx("0.0")
                .withCodes4xx("0.25")
                .withCodes5xx("0.25")
                .withTimeouts("0.25")
                .withOtherErrors("1.0")
                .withLag(60100)
                .build()

        when:
        SubscriptionHealth subscriptionHealth = subscriptionHealthChecker.checkHealth(ACTIVE_SUBSCRIPTION, topicMetrics, subscriptionMetrics)

        then:
        subscriptionHealth.status == UNHEALTHY
        subscriptionHealth.problems == [LAGGING, SLOW] as Set
    }

    def "should return healthy status for a healthy subscription when the topic is idle"() {
        given:
        def topicMetrics = topicMetricsWithRate("0.0")
        def subscriptionMetrics = otherwiseHealthySubscriptionMetrics().build()

        when:
        SubscriptionHealth subscriptionHealth = subscriptionHealthChecker.checkHealth(ACTIVE_SUBSCRIPTION, topicMetrics, subscriptionMetrics)

        then:
        subscriptionHealth == HEALTHY
    }

    def "should return 'no data' status and no problems when some of the metrics are unavailable even if others can be calculated"() {
        given:
        def topicMetrics = topicMetricsWithRate("100.0")
        def subscriptionMetrics = otherwiseHealthySubscriptionMetrics()
                .withRate("unavailable")
                .withLag(60100)
                .build()

        when:
        SubscriptionHealth subscriptionHealth = subscriptionHealthChecker.checkHealth(ACTIVE_SUBSCRIPTION, topicMetrics, subscriptionMetrics)

        then:
        subscriptionHealth == NO_DATA
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
    }
}
