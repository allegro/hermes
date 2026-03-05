package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate

import pl.allegro.tech.hermes.api.BatchSubscriptionPolicy
import pl.allegro.tech.hermes.api.Subscription
import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.common.metric.MaxRateMetrics
import pl.allegro.tech.hermes.common.metric.MetricsFacade
import pl.allegro.tech.hermes.consumers.subscription.cache.SubscriptionsCache
import pl.allegro.tech.hermes.consumers.supervisor.workload.ClusterAssignmentCache
import spock.lang.Specification

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

import static pl.allegro.tech.hermes.test.helper.builder.SubscriptionBuilder.subscription

class MaxRateCalculatorTest extends Specification {

    ClusterAssignmentCache clusterAssignmentCache = Mock(ClusterAssignmentCache)
    SubscriptionsCache subscriptionsCache = Mock(SubscriptionsCache)
    MaxRateBalancer balancer = Mock(MaxRateBalancer)
    MaxRateRegistry maxRateRegistry = Mock(MaxRateRegistry)
    MetricsFacade metrics = Mock(MetricsFacade)
    MaxRateMetrics maxRateMetrics = Mock(MaxRateMetrics)
    Clock clock = Clock.fixed(Instant.ofEpochMilli(0), ZoneId.systemDefault())

    MaxRateCalculator sut

    def setup() {
        metrics.maxRate() >> maxRateMetrics
        sut = new MaxRateCalculator(
                clusterAssignmentCache,
                subscriptionsCache,
                balancer,
                maxRateRegistry,
                metrics,
                clock
        )
    }

    def "should skip subscription when not found in subscriptions cache"() {
        given:
        SubscriptionName subscriptionName = SubscriptionName.fromString("group.topic\$sub")
        clusterAssignmentCache.getSubscriptionConsumers() >> [(subscriptionName): ["consumer1"] as Set]
        subscriptionsCache.getSubscription(subscriptionName) >> null

        when:
        sut.calculate()

        then:
        0 * maxRateRegistry.ensureCorrectAssignments(_, _)
        0 * balancer.balance(_, _)
        1 * maxRateRegistry.onBeforeMaxRateCalculation()
        1 * maxRateRegistry.onAfterMaxRateCalculation()
    }

    def "should calculate rates for serial subscription"() {
        given:
        Subscription sub = subscription("group.topic", "sub").build()
        SubscriptionName subscriptionName = sub.getQualifiedName()
        Set<String> consumerIds = ["consumer1"] as Set
        Set<ConsumerRateInfo> rateInfos = [new ConsumerRateInfo("consumer1", RateInfo.empty())] as Set
        Map<String, MaxRate> newRates = ["consumer1": new MaxRate(100d)]

        clusterAssignmentCache.getSubscriptionConsumers() >> [(subscriptionName): consumerIds]
        subscriptionsCache.getSubscription(subscriptionName) >> sub
        maxRateRegistry.ensureCorrectAssignments(subscriptionName, consumerIds) >> rateInfos
        balancer.balance(sub.getSerialSubscriptionPolicy().getRate(), rateInfos) >> Optional.of(newRates)

        when:
        sut.calculate()

        then:
        1 * maxRateRegistry.update(subscriptionName, newRates)
        1 * maxRateRegistry.onBeforeMaxRateCalculation()
        1 * maxRateRegistry.onAfterMaxRateCalculation()
    }

    def "should skip batch subscription"() {
        given:
        Subscription sub = subscription("group.topic", "sub")
                .withSubscriptionPolicy(
                        new BatchSubscriptionPolicy.Builder()
                                .withMessageTtl(100)
                                .withMessageBackoff(10)
                                .withBatchSize(100)
                                .withBatchTime(100)
                                .withBatchVolume(1024)
                                .withRequestTimeout(100)
                                .build()
                ).build()
        SubscriptionName subscriptionName = sub.getQualifiedName()

        clusterAssignmentCache.getSubscriptionConsumers() >> [(subscriptionName): ["consumer1"] as Set]
        subscriptionsCache.getSubscription(subscriptionName) >> sub

        when:
        sut.calculate()

        then:
        0 * maxRateRegistry.ensureCorrectAssignments(_, _)
        0 * balancer.balance(_, _)
        1 * maxRateRegistry.onBeforeMaxRateCalculation()
        1 * maxRateRegistry.onAfterMaxRateCalculation()
    }

    def "should continue processing other subscriptions when one is not in cache"() {
        given:
        SubscriptionName missingSub = SubscriptionName.fromString("group.topic\$missing")
        Subscription presentSub = subscription("group.topic", "present").build()
        SubscriptionName presentSubName = presentSub.getQualifiedName()
        Set<String> consumerIds = ["consumer1"] as Set
        Set<ConsumerRateInfo> rateInfos = [new ConsumerRateInfo("consumer1", RateInfo.empty())] as Set
        Map<String, MaxRate> newRates = ["consumer1": new MaxRate(100d)]

        clusterAssignmentCache.getSubscriptionConsumers() >> [
                (missingSub)    : consumerIds,
                (presentSubName): consumerIds
        ]
        subscriptionsCache.getSubscription(missingSub) >> null
        subscriptionsCache.getSubscription(presentSubName) >> presentSub
        maxRateRegistry.ensureCorrectAssignments(presentSubName, consumerIds) >> rateInfos
        balancer.balance(presentSub.getSerialSubscriptionPolicy().getRate(), rateInfos) >> Optional.of(newRates)

        when:
        sut.calculate()

        then:
        1 * maxRateRegistry.update(presentSubName, newRates)
        1 * maxRateRegistry.onBeforeMaxRateCalculation()
        1 * maxRateRegistry.onAfterMaxRateCalculation()
    }

    def "should continue processing other subscriptions when one fails with exception"() {
        given:
        Subscription failingSub = subscription("group.topic", "failing").build()
        SubscriptionName failingSubName = failingSub.getQualifiedName()
        Subscription successSub = subscription("group.topic", "success").build()
        SubscriptionName successSubName = successSub.getQualifiedName()
        Set<String> consumerIds = ["consumer1"] as Set
        Set<ConsumerRateInfo> rateInfos = [new ConsumerRateInfo("consumer1", RateInfo.empty())] as Set
        Map<String, MaxRate> newRates = ["consumer1": new MaxRate(100d)]

        clusterAssignmentCache.getSubscriptionConsumers() >> [
                (failingSubName): consumerIds,
                (successSubName): consumerIds
        ]
        subscriptionsCache.getSubscription(failingSubName) >> failingSub
        subscriptionsCache.getSubscription(successSubName) >> successSub
        maxRateRegistry.ensureCorrectAssignments(failingSubName, consumerIds) >> { throw new RuntimeException("ZK connection lost") }
        maxRateRegistry.ensureCorrectAssignments(successSubName, consumerIds) >> rateInfos
        balancer.balance(successSub.getSerialSubscriptionPolicy().getRate(), rateInfos) >> Optional.of(newRates)

        when:
        sut.calculate()

        then:
        1 * maxRateRegistry.update(successSubName, newRates)
        0 * maxRateRegistry.update(failingSubName, _)
        1 * maxRateRegistry.onBeforeMaxRateCalculation()
        1 * maxRateRegistry.onAfterMaxRateCalculation()
    }
}
