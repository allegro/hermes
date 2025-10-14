package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import pl.allegro.tech.hermes.consumers.config.GoogleBigQueryThreadPoolProperties;

public class ThreadPoolProvider {
  ScheduledExecutorService scheduledExecutorService;

  public ThreadPoolProvider(GoogleBigQueryThreadPoolProperties googleBigQueryThreadPoolProperties) {
    this.scheduledExecutorService =
        Executors.newScheduledThreadPool(googleBigQueryThreadPoolProperties.getPoolSize());
  }

  public java.util.concurrent.ScheduledExecutorService getExecutorService() {
    return scheduledExecutorService;
  }
}
