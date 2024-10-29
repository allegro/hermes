package pl.allegro.tech.hermes.frontend.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "frontend.startup.topic.loading")
public class TopicLoadingProperties {

  private MetadataLoadingProperties metadata = new MetadataLoadingProperties();

  private SchemaLoadingProperties schema = new SchemaLoadingProperties();

  private MetadataRefreshJobProperties metadataRefreshJob = new MetadataRefreshJobProperties();

  public static class MetadataLoadingProperties {

    private Duration retryInterval = Duration.ofSeconds(1);

    private int retryCount = 5;

    private int threadPoolSize = 16;

    public Duration getRetryInterval() {
      return retryInterval;
    }

    public void setRetryInterval(Duration retryInterval) {
      this.retryInterval = retryInterval;
    }

    public int getRetryCount() {
      return retryCount;
    }

    public void setRetryCount(int retryCount) {
      this.retryCount = retryCount;
    }

    public int getThreadPoolSize() {
      return threadPoolSize;
    }

    public void setThreadPoolSize(int threadPoolSize) {
      this.threadPoolSize = threadPoolSize;
    }
  }

  public static class SchemaLoadingProperties {

    private boolean enabled = false;

    private int retryCount = 3;

    private int threadPoolSize = 16;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public int getRetryCount() {
      return retryCount;
    }

    public void setRetryCount(int retryCount) {
      this.retryCount = retryCount;
    }

    public int getThreadPoolSize() {
      return threadPoolSize;
    }

    public void setThreadPoolSize(int threadPoolSize) {
      this.threadPoolSize = threadPoolSize;
    }
  }

  public static class MetadataRefreshJobProperties {

    private boolean enabled = true;

    private Duration interval = Duration.ofSeconds(60);

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public Duration getInterval() {
      return interval;
    }

    public void setInterval(Duration interval) {
      this.interval = interval;
    }
  }

  public MetadataLoadingProperties getMetadata() {
    return metadata;
  }

  public void setMetadata(MetadataLoadingProperties metadata) {
    this.metadata = metadata;
  }

  public SchemaLoadingProperties getSchema() {
    return schema;
  }

  public void setSchema(SchemaLoadingProperties schema) {
    this.schema = schema;
  }

  public MetadataRefreshJobProperties getMetadataRefreshJob() {
    return metadataRefreshJob;
  }

  public void setMetadataRefreshJob(MetadataRefreshJobProperties metadataRefreshJob) {
    this.metadataRefreshJob = metadataRefreshJob;
  }
}
