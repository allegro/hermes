package pl.allegro.tech.hermes.consumers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "consumer.google.bigquery.avro.writer")
public class GoogeBigQueryAvroStreamWriterProperties {

  private int poolSize = 100;
  private int keepAliveTimeSeconds = 60;
  private int channelPoolStaticSize = 2;

  public int getPoolSize() {
    return poolSize;
  }

  public int getKeepAliveTimeSeconds() {
    return keepAliveTimeSeconds;
  }

  public void setKeepAliveTimeSeconds(int keepAliveTimeSeconds) {
    this.keepAliveTimeSeconds = keepAliveTimeSeconds;
  }

  public void setPoolSize(int poolSize) {
    this.poolSize = poolSize;
  }

  public int getChannelPoolStaticSize() {
    return channelPoolStaticSize;
  }

  public void setChannelPoolStaticSize(int channelPoolStaticSize) {
    this.channelPoolStaticSize = channelPoolStaticSize;
  }
}
