package pl.allegro.tech.hermes.consumers.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "consumer.http-client.monitoring")
public class HttpClientsMonitoringProperties {

  private boolean connectionPoolMonitoringEnabled = false;

  private boolean requestQueueMonitoringEnabled = true;

  public boolean isConnectionPoolMonitoringEnabled() {
    return connectionPoolMonitoringEnabled;
  }

  public void setConnectionPoolMonitoringEnabled(boolean connectionPoolMonitoringEnabled) {
    this.connectionPoolMonitoringEnabled = connectionPoolMonitoringEnabled;
  }

  public boolean isRequestQueueMonitoringEnabled() {
    return requestQueueMonitoringEnabled;
  }

  public void setRequestQueueMonitoringEnabled(boolean requestQueueMonitoringEnabled) {
    this.requestQueueMonitoringEnabled = requestQueueMonitoringEnabled;
  }
}
