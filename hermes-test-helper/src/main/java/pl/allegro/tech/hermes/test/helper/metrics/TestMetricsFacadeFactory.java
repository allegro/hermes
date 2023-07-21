package pl.allegro.tech.hermes.test.helper.metrics;

import com.codahale.metrics.MetricRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.metrics.PathsCompiler;

public class TestMetricsFacadeFactory {

    public static MetricsFacade create() {
        return new MetricsFacade(
                new SimpleMeterRegistry(),
                new HermesMetrics(new MetricRegistry(), new PathsCompiler("localhost"))
        );
    }
}
