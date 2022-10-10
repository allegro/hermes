package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted

import com.codahale.metrics.MetricRegistry
import pl.allegro.tech.hermes.api.SubscriptionName
import pl.allegro.tech.hermes.common.metric.HermesMetrics
import pl.allegro.tech.hermes.metrics.PathsCompiler
import spock.lang.Specification
import spock.lang.Subject

import java.time.Clock
import java.time.Duration
import java.time.Instant

class ScoringTargetWeightCalculatorTest extends Specification {

    def hermesMetrics = new HermesMetrics(new MetricRegistry(), new PathsCompiler("host"))
    def scoringGain = 1.0d

    @Subject
    def calculator = new ScoringTargetWeightCalculator(
            new WeightedWorkloadMetrics(hermesMetrics),
            Clock.systemDefaultZone(),
            Duration.ofMinutes(15),
            scoringGain
    )

    def "should assign weights accordingly to consumer performance"() {
        given:
        def consumers = [
                consumerNode("c1", 0.5, new Weight(100)),
                consumerNode("c2", 1.0, new Weight(50)),
                consumerNode("c3", 0.6, new Weight(50))
        ]

        when:
        def weights = calculator.calculate(consumers)

        then:
        // target CPU utilization = (0.5 + 1.0 + 0.6) / 3 = 0.7
        // CPU utilization errors:
        // c1 = 0.7 - 0.5 = 0.2
        // c2 = 0.7 - 1.0 = -0.3
        // c3 = 0.7 - 0.6 = 0.1
        //
        // total weight = 100 + 50 + 50 = 200
        // current scores:
        // c1 = 100 / 200 = 0.5
        // c2 = 50 / 200 = 0.25
        // c3 = 50 / 200 = 0.25
        //
        // new scores (current score + scoringGain * error)
        // c1 = 0.5 + 1.0 * 0.2 = 0.7
        // c2 = 0.25 + 1.0 * (-0.3) = 0.01 (min score)
        // c3 = 0.25 + 1.0 * 0.1 = 0.35
        // new scores sum = 0.7 + 0.01 + 0.35 = 1.06
        //
        // new weights (total weight * (new score / new scores sum)):
        // c1 = 200 * 0.7 / 1.06 ~ 132
        // c2 = 200 * 0.01 / 1.06 ~ 1.88
        // c3 = 200 * 0.35 / 1.06 ~ 66
        def expected = [
                "c1": new Weight(132.08),
                "c2": new Weight(1.89),
                "c3": new Weight(66.04)
        ]
        areClose(expected, weights)
    }

    def "should not change correct weights"() {
        given:
        def consumers = [
                consumerNode("c1", 0.5, new Weight(100)),
                consumerNode("c2", 0.5, new Weight(70)),
                consumerNode("c3", 0.5, new Weight(90))
        ]

        when:
        def weights = calculator.calculate(consumers)

        then:
        def expected = [
                "c1": new Weight(100),
                "c2": new Weight(70),
                "c3": new Weight(90)
        ]
        areClose(expected, weights)
    }

    def "should use weight average when consumers have undefined load"() {
        given:
        def consumers = [
                consumerNode("c1", 0.7, new Weight(180)),
                consumerNode("c2", 0.4, new Weight(60)),
                consumerNodeWithUndefinedLoad("c3")
        ]

        when:
        def weights = calculator.calculate(consumers)

        then:
        def expected = [
                "c1": new Weight(96.0),
                "c2": new Weight(64.0),
                "c3": new Weight(80.0)
        ]
        areClose(expected, weights)
    }

    def "should use weight average when consumers have subscriptions assigned and undefined CPU utilization"() {
        given:
        def consumers = [
                consumerNode("c1", ConsumerNodeLoad.UNDEFINED.cpuUtilization, new Weight(100)),
                consumerNode("c2", ConsumerNodeLoad.UNDEFINED.cpuUtilization, new Weight(60)),
                consumerNode("c3", ConsumerNodeLoad.UNDEFINED.cpuUtilization, new Weight(80)),
        ]

        when:
        def weights = calculator.calculate(consumers)

        then:
        def expected = [
                "c1": new Weight(80),
                "c2": new Weight(80),
                "c3": new Weight(80)
        ]
        areClose(expected, weights)
    }

    def "should assign weight zero to all consumers when there are no subscriptions"() {
        given:
        def consumers = [
                consumerNodeWithUndefinedLoad("c1"),
                consumerNodeWithUndefinedLoad("c2"),
                consumerNodeWithUndefinedLoad("c3")
        ]

        when:
        def weights = calculator.calculate(consumers)

        then:
        def expected = [
                "c1": Weight.ZERO,
                "c2": Weight.ZERO,
                "c3": Weight.ZERO
        ]
        areClose(expected, weights)
    }

    def "should return empty map when consumer list is empty"() {
        given:
        def consumers = []

        when:
        def weights = calculator.calculate(consumers)

        then:
        weights == [:]
    }

    private static boolean areClose(Map<String, Weight> expected, Map<String, Weight> actual, Float eps = 1e-2) {
        if (expected.keySet() != actual.keySet()) return false
        return expected.every { consumer, weight -> isClose(weight, actual[consumer], eps) }
    }

    private static boolean isClose(Weight a, Weight b, Float eps) {
        return Math.abs(a.getOperationsPerSecond() - b.getOperationsPerSecond()) < eps
    }


    private static ConsumerNode consumerNode(String consumerId, double cpu, Weight weight) {
        def subscription = SubscriptionName.fromString("pl.allegro.tech.hermes\$sub1")
        def subscriptions = Map.of(
                subscription, new SubscriptionLoad(weight.getOperationsPerSecond())
        )
        ConsumerNode consumerNode = new ConsumerNode(consumerId, new ConsumerNodeLoad(cpu, subscriptions), 3)
        consumerNode.assign(new ConsumerTask(subscription, new SubscriptionProfile(Instant.now(), weight)))
        return consumerNode
    }

    private static ConsumerNode consumerNodeWithUndefinedLoad(String consumerId) {
        return new ConsumerNode(consumerId, ConsumerNodeLoad.UNDEFINED, 3)
    }
}
