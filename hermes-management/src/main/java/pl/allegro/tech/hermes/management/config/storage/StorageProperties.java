package pl.allegro.tech.hermes.management.config.storage;

public class StorageProperties {
  private String datacenter = "dc";
  private String clusterName = "zk";
  private String connectionString = "localhost:2181";
  private int sessionTimeout = 10000;
  private int connectTimeout = 1000;

  public String getConnectionString() {
    return connectionString;
  }

  public void setConnectionString(String connectionString) {
    this.connectionString = connectionString;
  }

  public int getSessionTimeout() {
    return sessionTimeout;
  }

  public void setSessionTimeout(int sessionTimeout) {
    this.sessionTimeout = sessionTimeout;
  }

  public int getConnectTimeout() {
    return connectTimeout;
  }

  public void setConnectTimeout(int connectTimeout) {
    this.connectTimeout = connectTimeout;
  }

  public String getDatacenter() {
    return datacenter;
  }

  public void setDatacenter(String datacenter) {
    this.datacenter = datacenter;
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }
}
