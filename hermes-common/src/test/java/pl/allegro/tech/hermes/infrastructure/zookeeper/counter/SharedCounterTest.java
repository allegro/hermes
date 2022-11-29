package pl.allegro.tech.hermes.infrastructure.zookeeper.counter;

import com.codahale.metrics.MetricRegistry;
import org.junit.Before;
import org.junit.Test;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.metrics.PathsCompiler;
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperBaseTest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.common.metric.Meters.SHARED_COUNTERS_OPTIMISTIC_INCREMENT_METER;
import static pl.allegro.tech.hermes.common.metric.Meters.SHARED_COUNTERS_PESSIMISTIC_INCREMENT_METER;

public class SharedCounterTest extends ZookeeperBaseTest {

    private HermesMetrics hermesMetrics;
    private SharedCounter counter;

    @Before
    public void initialize() {
        this.hermesMetrics = new HermesMetrics(new MetricRegistry(), new PathsCompiler("host"));
        this.counter = new SharedCounter(zookeeperClient, Duration.ofHours(72), Duration.ofSeconds(1), 3, hermesMetrics);
    }

    @Test
    public void shouldIncrementAndRetrieveCounterForGivenPath() {
        // given when
        counter.increment("/increment", 10);
        wait.untilZookeeperPathIsCreated("/increment");

        // then
        assertThat(counter.getValue("/increment")).isEqualTo(10);
        assertThatMetricsHaveBeenReported(hermesMetrics);
    }

    @Test
    public void shouldIncrementCounterAtomicallyWhenIncrementedConcurrently() {
        // given
        HermesMetrics otherHermesMetrics = new HermesMetrics(new MetricRegistry(), new PathsCompiler("host"));
        SharedCounter otherCounter = new SharedCounter(zookeeperClient, Duration.ofHours(72), Duration.ofSeconds(1), 3, otherHermesMetrics);

        // when
        counter.increment("/sharedIncrement", 10);
        otherCounter.increment("/sharedIncrement", 15);
        wait.untilZookeeperPathIsCreated("/sharedIncrement");

        // then
        assertThat(counter.getValue("/sharedIncrement")).isEqualTo(25);
        assertThatMetricsHaveBeenReported(otherHermesMetrics);
        assertThatMetricsHaveBeenReported(hermesMetrics);
    }

    private static void assertThatMetricsHaveBeenReported(HermesMetrics hermesMetrics) {
        long pessimistic = hermesMetrics.meter(SHARED_COUNTERS_PESSIMISTIC_INCREMENT_METER).getCount();
        long optimistic = hermesMetrics.meter(SHARED_COUNTERS_OPTIMISTIC_INCREMENT_METER).getCount();
        assertThat(pessimistic + optimistic).isGreaterThan(0);
    }
}
