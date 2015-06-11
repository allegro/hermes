package pl.allegro.tech.hermes.common.metric.executor;

import pl.allegro.tech.hermes.common.metric.HermesMetrics;

import javax.inject.Inject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.Executors.newScheduledThreadPool;

public class InstrumentedExecutorServiceFactory {

    private final HermesMetrics hermesMetrics;

    @Inject
    public InstrumentedExecutorServiceFactory(HermesMetrics hermesMetrics) {
        this.hermesMetrics = hermesMetrics;
    }

    public ExecutorService getExecutorService(String name, int size, boolean monitoringEnabled) {
        ExecutorService executor = Executors.newFixedThreadPool(size);
        if (monitoringEnabled) {
            return new InstrumentedExecutorService(executor, hermesMetrics, name);
        } else {
            return executor;
        }
    }

    public ScheduledExecutorService getScheduledExecutorService(String name, int size, boolean monitoringEnabled) {
        ScheduledExecutorService executor = newScheduledThreadPool(size);
        if (monitoringEnabled) {
            return new InstrumentedScheduledExecutorService(executor, hermesMetrics, name);
        } else {
            return executor;
        }
    }

}
