package pl.allegro.tech.hermes.consumers.consumer.sender.googlebigquery;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import pl.allegro.tech.hermes.consumers.config.GoogleBigQueryThreadPoolProperties;

public class ThreadPoolProvider {
  ScheduledExecutorService scheduledExecutorService;
  GoogleBigQueryThreadPoolProperties googleBigQueryThreadPoolProperties;

  public ThreadPoolProvider(GoogleBigQueryThreadPoolProperties googleBigQueryThreadPoolProperties) {
    this.googleBigQueryThreadPoolProperties = googleBigQueryThreadPoolProperties;
    this.scheduledExecutorService = newScheduledExecutorService();
  }

  private ScheduledExecutorService newScheduledExecutorService() {
    return Executors.newScheduledThreadPool(googleBigQueryThreadPoolProperties.getPoolSize());
  }

  public java.util.concurrent.ScheduledExecutorService getExecutorService() {
    if (googleBigQueryThreadPoolProperties.isCommonThreadPool()) {
      return scheduledExecutorService;
    } else {
      return newScheduledExecutorService();
    }
  }
}
