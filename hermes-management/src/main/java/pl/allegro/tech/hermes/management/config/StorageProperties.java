package pl.allegro.tech.hermes.management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    private String pathPrefix = "/hermes";
    private String connectionString = "localhost:2181";
    private int sessionTimeout = 10000;
    private int connectTimeout = 1000;
    private int retryTimes = 3;
    private int retrySleep = 1000;
    private int sharedCountersExpiration = 72;

    public String getPathPrefix() {
        return pathPrefix;
    }

    public void setPathPrefix(String pathPrefix) {
        this.pathPrefix = pathPrefix;
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
}
