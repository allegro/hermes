package pl.allegro.tech.hermes.test.helper.concurrent;

import pl.allegro.tech.hermes.common.concurrent.ExecutorServiceFactory;

import java.util.concurrent.ScheduledExecutorService;

public class TestExecutorServiceFactory implements ExecutorServiceFactory {

    private final ScheduledExecutorService scheduledExecutorService;

    public TestExecutorServiceFactory(ScheduledExecutorService scheduledExecutorService) {
        this.scheduledExecutorService = scheduledExecutorService;
    }

    @Override
    public ScheduledExecutorService createSingleThreadScheduledExecutor(String nameFormat) {
        return scheduledExecutorService;
    }
}
