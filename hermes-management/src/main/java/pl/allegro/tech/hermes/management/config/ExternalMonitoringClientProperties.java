package pl.allegro.tech.hermes.management.config;

public class ExternalMonitoringClientProperties {

  private int connectionTimeoutMillis = 1000;

  private int socketTimeoutMillis = 2000;

  private int maxConnections = 100;

  private int maxConnectionsPerRoute = 100;

  private int cacheTtlSeconds = 55;

  private int cacheSize = 100_000;

  private int fetchingTimeoutMillis = 5000;
  private int fetchingThreads = 30;

  private String externalMonitoringUrl = "http://localhost:18090";

  public int getConnectionTimeoutMillis() {
    return connectionTimeoutMillis;
  }

  public void setConnectionTimeoutMillis(int connectionTimeoutMillis) {
    this.connectionTimeoutMillis = connectionTimeoutMillis;
  }

  public int getSocketTimeoutMillis() {
    return socketTimeoutMillis;
  }

  public void setSocketTimeoutMillis(int socketTimeoutMillis) {
    this.socketTimeoutMillis = socketTimeoutMillis;
  }

  public int getCacheTtlSeconds() {
    return cacheTtlSeconds;
  }

  public void setCacheTtlSeconds(int cacheTtlSeconds) {
    this.cacheTtlSeconds = cacheTtlSeconds;
  }

  public int getCacheSize() {
    return cacheSize;
  }

  public void setCacheSize(int cacheSize) {
    this.cacheSize = cacheSize;
  }

  public int getMaxConnections() {
    return maxConnections;
  }

  public void setMaxConnections(int maxConnections) {
    this.maxConnections = maxConnections;
  }

  public int getMaxConnectionsPerRoute() {
    return maxConnectionsPerRoute;
  }

  public void setMaxConnectionsPerRoute(int maxConnectionsPerRoute) {
    this.maxConnectionsPerRoute = maxConnectionsPerRoute;
  }

  public String getExternalMonitoringUrl() {
    return externalMonitoringUrl;
  }

  public void setExternalMonitoringUrl(String externalMonitoringUrl) {
    this.externalMonitoringUrl = externalMonitoringUrl;
  }

  public int getFetchingThreads() {
    return fetchingThreads;
  }

  public void setFetchingThreads(int fetchingThreads) {
    this.fetchingThreads = fetchingThreads;
  }

  public int getFetchingTimeoutMillis() {
    return fetchingTimeoutMillis;
  }

  public void setFetchingTimeoutMillis(int fetchingTimeoutMillis) {
    this.fetchingTimeoutMillis = fetchingTimeoutMillis;
  }
}
