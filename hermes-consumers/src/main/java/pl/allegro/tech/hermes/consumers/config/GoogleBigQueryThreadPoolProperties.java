package pl.allegro.tech.hermes.consumers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "consumer.google.bigquery.thread-pool")
public class GoogleBigQueryThreadPoolProperties {
  boolean commonThreadPool = true;
  int poolSize = 100;

  public int getPoolSize() {
    return poolSize;
  }

  public void setPoolSize(int poolSize) {
    this.poolSize = poolSize;
  }

  public boolean isCommonThreadPool() {
    return commonThreadPool;
  }

  public void setCommonThreadPool(boolean commonThreadPool) {
    this.commonThreadPool = commonThreadPool;
  }
}
