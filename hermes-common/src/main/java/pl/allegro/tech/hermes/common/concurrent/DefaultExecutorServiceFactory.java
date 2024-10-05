package pl.allegro.tech.hermes.common.concurrent;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class DefaultExecutorServiceFactory implements ExecutorServiceFactory {

  @Override
  public ScheduledExecutorService createSingleThreadScheduledExecutor(String nameFormat) {
    return Executors.newSingleThreadScheduledExecutor(
        new ThreadFactoryBuilder().setNameFormat(nameFormat).build());
  }
}
