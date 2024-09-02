package pl.allegro.tech.hermes.common.concurrent;

import java.util.concurrent.ScheduledExecutorService;

public interface ExecutorServiceFactory {

  ScheduledExecutorService createSingleThreadScheduledExecutor(String nameFormat);
}
