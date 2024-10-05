package pl.allegro.tech.hermes.management.config.storage;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import pl.allegro.tech.hermes.infrastructure.dc.DcNameSource;

@ConfigurationProperties(prefix = "storage")
public class StorageClustersProperties {

  private String pathPrefix = "/hermes";
  private int retryTimes = 3;
  private int retrySleep = 1000;
  private int sharedCountersExpiration = 72;
  private DcNameSource datacenterNameSource;
  private String datacenterNameSourceEnv = "DC";
  private boolean transactional = true;
  private int maxConcurrentOperations = 100;
  private String connectionString = "localhost:2181";
  private int sessionTimeout = 10000;
  private int connectTimeout = 1000;
  private List<StorageProperties> clusters = new ArrayList<>();

  @NestedConfigurationProperty private StorageAuthorizationProperties authorization;

  public String getPathPrefix() {
    return pathPrefix;
  }

  public void setPathPrefix(String pathPrefix) {
    this.pathPrefix = pathPrefix;
  }

  public int getRetryTimes() {
    return retryTimes;
  }

  public void setRetryTimes(int retryTimes) {
    this.retryTimes = retryTimes;
  }

  public int getRetrySleep() {
    return retrySleep;
  }

  public void setRetrySleep(int retrySleep) {
    this.retrySleep = retrySleep;
  }

  public int getSharedCountersExpiration() {
    return sharedCountersExpiration;
  }

  public void setSharedCountersExpiration(int sharedCountersExpiration) {
    this.sharedCountersExpiration = sharedCountersExpiration;
  }

  public StorageAuthorizationProperties getAuthorization() {
    return authorization;
  }

  public void setAuthorization(StorageAuthorizationProperties authorization) {
    this.authorization = authorization;
  }

  public List<StorageProperties> getClusters() {
    return clusters;
  }

  public void setClusters(List<StorageProperties> clusters) {
    this.clusters = clusters;
  }

  public DcNameSource getDatacenterNameSource() {
    return datacenterNameSource;
  }

  public void setDatacenterNameSource(DcNameSource datacenterNameSource) {
    this.datacenterNameSource = datacenterNameSource;
  }

  public String getDatacenterNameSourceEnv() {
    return datacenterNameSourceEnv;
  }

  public void setDatacenterNameSourceEnv(String datacenterNameSourceEnv) {
    this.datacenterNameSourceEnv = datacenterNameSourceEnv;
  }

  public boolean isTransactional() {
    return transactional;
  }

  public void setTransactional(boolean transactional) {
    this.transactional = transactional;
  }

  public int getMaxConcurrentOperations() {
    return maxConcurrentOperations;
  }

  public void setMaxConcurrentOperations(int maxConcurrentOperations) {
    this.maxConcurrentOperations = maxConcurrentOperations;
  }

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
}
