package pl.allegro.tech.hermes.common.metric.executor

import com.codahale.metrics.MetricRegistry
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.search.Search
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import pl.allegro.tech.hermes.common.metric.HermesMetrics
import pl.allegro.tech.hermes.common.metric.MetricsFacade
import pl.allegro.tech.hermes.metrics.PathsCompiler
import spock.lang.Specification
import spock.lang.Subject

import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture

import static java.util.concurrent.TimeUnit.SECONDS

class InstrumentedExecutorServiceFactoryMetricsTest extends Specification {

    private final MeterRegistry meterRegistry = new SimpleMeterRegistry()

    @Subject
    private final InstrumentedExecutorServiceFactory factory =
            new InstrumentedExecutorServiceFactory(
                    new MetricsFacade(
                            meterRegistry,
                            new HermesMetrics(new MetricRegistry(), new PathsCompiler("host"))
                    )
            )

    def "should record metrics for executor service (monitoring enabled: #monitoringEnabled)"() {
        given:
        ExecutorService executor = factory.getExecutorService("test-executor", 10, monitoringEnabled)

        when:
        Future<?> task = executor.submit { println("task executed") }
        task.get()

        then:
        metric("executor.completed", "test-executor").functionCounter()?.count() == expectedCompletedCount

        where:
        monitoringEnabled || expectedCompletedCount
        true              || 1.0d
        false             || null
    }

    def "should record metrics for scheduled executor service (monitoring enabled: #monitoringEnabled)"() {
        given:
        ScheduledExecutorService executor = factory.scheduledExecutorBuilder("test-scheduled-executor", 10)
                .withMonitoringEnabled(true).create()

        when:
        ScheduledFuture<?> task = executor.schedule({ println("scheduled task executed") }, 1, SECONDS)
        task.get()

        then:
        metric("executor.completed", "test-scheduled-executor").functionCounter()?.count() == expectedCompletedCount

        where:
        monitoringEnabled || expectedCompletedCount
        true              || 1.0d
        false             || null
    }

    private Search metric(String name, String executorName) {
        return meterRegistry.find(name).tag("name", executorName)
    }
}
