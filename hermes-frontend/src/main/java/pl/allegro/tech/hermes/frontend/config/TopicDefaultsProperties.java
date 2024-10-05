package pl.allegro.tech.hermes.frontend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "frontend.topic.defaults")
public class TopicDefaultsProperties {
  private boolean fallbackToRemoteDatacenterEnabled = false;

  public boolean isFallbackToRemoteDatacenterEnabled() {
    return fallbackToRemoteDatacenterEnabled;
  }

  public void setFallbackToRemoteDatacenterEnabled(boolean fallbackToRemoteDatacenterEnabled) {
    this.fallbackToRemoteDatacenterEnabled = fallbackToRemoteDatacenterEnabled;
  }
}
