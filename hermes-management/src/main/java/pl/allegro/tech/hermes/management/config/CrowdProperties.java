package pl.allegro.tech.hermes.management.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URL;

@ConfigurationProperties(prefix = "owner.crowd")
public class CrowdProperties {

    private boolean enabled = false;

    private String userName;

    private String password;

    private URL path;

    private Long cacheDurationSeconds = 300L;

    private Long cacheSize = 1000L;

    private int connectionTimeoutMillis = 1000;

    private int socketTimeoutMillis = 2000;

    private int maxConnections = 100;

    private int maxConnectionsPerRoute = 10;


    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public URL getPath() {
        return path;
    }

    public void setPath(URL path) {
        this.path = path;
    }

    public Long getCacheDurationSeconds() {
        return cacheDurationSeconds;
    }

    public void setCacheDurationSeconds(Long cacheDurationSeconds) {
        this.cacheDurationSeconds = cacheDurationSeconds;
    }

    public Long getCacheSize() {
        return cacheSize;
    }

    public void setCacheSize(Long cacheSize) {
        this.cacheSize = cacheSize;
    }

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
}
