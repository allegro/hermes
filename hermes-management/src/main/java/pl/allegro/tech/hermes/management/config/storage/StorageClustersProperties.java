package pl.allegro.tech.hermes.management.config.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import pl.allegro.tech.hermes.common.di.factories.ZookeeperParameters;
import pl.allegro.tech.hermes.infrastructure.dc.DcNameSource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "storage")
public class StorageClustersProperties implements ZookeeperParameters {

    private String pathPrefix = "/hermes";
    private int retryTimes = 3;
    private int retrySleep = 1000;
    private Duration maxSleepTime = Duration.ofSeconds(30);
    private int sharedCountersExpiration = 72;
    private DcNameSource datacenterNameSource;
    private String datacenterNameSourceEnv = "DC";
    private boolean transactional = true;
    private int maxConcurrentOperations = 100;
    private String connectionString = "localhost:2181";
    private int sessionTimeout = 10000;
    private int connectTimeout = 1000;
    private int processingThreadPoolSize = 5;

    private List<StorageProperties> clusters = new ArrayList<>();

    @NestedConfigurationProperty
    private StorageAuthorizationProperties authorization;

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

    public Duration getSessionTimeout() {
        return Duration.ofMillis(sessionTimeout);
    }

    public void setSessionTimeout(int sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public void setMaxSleepTime(Duration maxSleepTime) {
        this.maxSleepTime = maxSleepTime;
    }

    public void setProcessingThreadPoolSize(int processingThreadPoolSize) {
        this.processingThreadPoolSize = processingThreadPoolSize;
    }

    @Override
    public Duration getBaseSleepTime() {
        return Duration.ofMillis(retrySleep);
    }

    @Override
    public Duration getMaxSleepTime() {
        return maxSleepTime;
    }

    @Override
    public int getMaxRetries() {
        return retryTimes;
    }

    @Override
    public Duration getConnectionTimeout() {
        return Duration.ofMillis(connectTimeout);
    }

    @Override
    public String getRoot() {
        return pathPrefix;
    }

    @Override
    public int getProcessingThreadPoolSize() {
        return processingThreadPoolSize;
    }

    @Override
    public boolean isAuthorizationEnabled() {
        return authorization != null;
    }

    @Override
    public String getScheme() {
        return authorization.getScheme();
    }

    @Override
    public String getUser() {
        return authorization.getUser();
    }

    @Override
    public String getPassword() {
        return authorization.getPassword();
    }
}
