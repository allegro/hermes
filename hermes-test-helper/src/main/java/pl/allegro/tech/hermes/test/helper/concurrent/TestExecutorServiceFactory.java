package pl.allegro.tech.hermes.test.helper.concurrent;

import java.util.concurrent.ScheduledExecutorService;
import pl.allegro.tech.hermes.common.concurrent.ExecutorServiceFactory;

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
