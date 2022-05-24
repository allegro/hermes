package pl.allegro.tech.hermes.common.metric.executor;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import static java.util.concurrent.Executors.newScheduledThreadPool;

public class InstrumentedExecutorServiceFactory {

    private final HermesMetrics hermesMetrics;

    public InstrumentedExecutorServiceFactory(HermesMetrics hermesMetrics) {
        this.hermesMetrics = hermesMetrics;
    }

    public ExecutorService getExecutorService(String name, int size, boolean monitoringEnabled) {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat(name + "-executor-%d").build();
        ExecutorService executor = Executors.newFixedThreadPool(size, threadFactory);
        if (monitoringEnabled) {
            return new InstrumentedExecutorService(executor, hermesMetrics, name);
        } else {
            return executor;
        }
    }

    public ScheduledExecutorService getScheduledExecutorService(String name, int size, boolean monitoringEnabled) {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat(name + "-scheduled-executor-%d").build();
        ScheduledExecutorService executor = newScheduledThreadPool(size, threadFactory);
        if (monitoringEnabled) {
            return new InstrumentedScheduledExecutorService(executor, hermesMetrics, name);
        } else {
            return executor;
        }
    }

}
