package pl.allegro.tech.hermes.frontend.publishing.metric

import com.codahale.metrics.MetricRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions
import org.awaitility.Awaitility
import pl.allegro.tech.hermes.api.Topic
import pl.allegro.tech.hermes.common.metric.MetricsFacade
import pl.allegro.tech.hermes.frontend.metric.ThroughputMeter
import pl.allegro.tech.hermes.frontend.metric.ThroughputRegistry
import spock.lang.Shared
import spock.lang.Specification

import java.time.Duration

import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic

class ThroughputRegistryTest extends Specification {
    @Shared
    Topic topicA = topic("group.topicA").build()
    @Shared
    Topic topicB = topic("group.topicB").build()
    @Shared
    Topic topicC = topic("group.topicC").build();

    private final MetricsFacade metricsFacade = new MetricsFacade(new SimpleMeterRegistry());
    private final ThroughputRegistry throughputRegistry = new ThroughputRegistry(metricsFacade, new MetricRegistry())

    def "topic throughput should be preserved for instances of the same topic"() {
        given: "throughput meter for a topic with recorded value"
        ThroughputMeter meter = throughputRegistry.forTopic(topicA.getName())
        assert meter.oneMinuteRate == 0.0d

        meter.increment(1024)

        when: "new throughput meter is obtained for the same topic"
        meter = throughputRegistry.forTopic(topicA.getName())

        then: "throughput is preserved"
        Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted {
            Assertions.assertThat(meter.oneMinuteRate).isGreaterThan(0.0d)
        }
    }

    def "global throughput should be shared for all topics"() {
        given: "given throughput meters for two topics"
        ThroughputMeter topicBMeter = throughputRegistry.forTopic(topicB.getName())
        ThroughputMeter topicCMeter = throughputRegistry.forTopic(topicC.getName())
        assert topicBMeter.oneMinuteRate == 0.0d
        assert topicCMeter.oneMinuteRate == 0.0d

        when: "throughput for both meters is recorded"
        topicBMeter.increment(1024)
        topicCMeter.increment(1024)

        then: "global throughput is a sum of topic throughput"
        Awaitility.await().atMost(Duration.ofSeconds(10)).untilAsserted {
            def topicAValue = topicBMeter.oneMinuteRate
            def topicBValue = topicCMeter.oneMinuteRate
            def globalValue = throughputRegistry.globalThroughputOneMinuteRate
            Assertions.assertThat(topicAValue).isGreaterThan(0.0d)
            Assertions.assertThat(topicBValue).isGreaterThan(0.0d)
            Assertions.assertThat(globalValue).isGreaterThan(topicAValue)
        }
    }

}
