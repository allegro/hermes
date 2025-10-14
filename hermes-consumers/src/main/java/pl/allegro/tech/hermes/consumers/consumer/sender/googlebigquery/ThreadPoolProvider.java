package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery;

import pl.allegro.tech.hermes.consumers.config.GoogleBigQueryThreadPoolProperties;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

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
