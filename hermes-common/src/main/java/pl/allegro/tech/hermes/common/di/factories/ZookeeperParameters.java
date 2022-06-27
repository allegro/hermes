package pl.allegro.tech.hermes.common.di.factories;

public class ZookeeperParameters {

    private final String connectionString;

    private final int baseSleepTime;

    private final int maxSleepTimeSeconds;

    private final int maxRetries;

    private final int connectionTimeout;

    private final int sessionTimeout;

    private final String root;

    private final int processingThreadPoolSize;

    private final boolean authorizationEnabled;

    private final String scheme;

    private final String user;

    private final String password;

    public String getConnectionString() {
        return connectionString;
    }

    public int getBaseSleepTime() {
        return baseSleepTime;
    }

    public int getMaxSleepTimeSeconds() {
        return maxSleepTimeSeconds;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public int getSessionTimeout() {
        return sessionTimeout;
    }

    public String getRoot() {
        return root;
    }

    public int getProcessingThreadPoolSize() {
        return processingThreadPoolSize;
    }

    public boolean isAuthorizationEnabled() {
        return authorizationEnabled;
    }

    public String getScheme() {
        return scheme;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public ZookeeperParameters(String connectionString,
                               int baseSleepTime,
                               int maxSleepTimeSeconds,
                               int maxRetries,
                               int connectionTimeout,
                               int sessionTimeout,
                               String root,
                               int processingThreadPoolSize,
                               boolean authorizationEnabled,
                               String scheme,
                               String user,
                               String password) {
        this.connectionString = connectionString;
        this.baseSleepTime = baseSleepTime;
        this.maxSleepTimeSeconds = maxSleepTimeSeconds;
        this.maxRetries = maxRetries;
        this.connectionTimeout = connectionTimeout;
        this.sessionTimeout = sessionTimeout;
        this.root = root;
        this.processingThreadPoolSize = processingThreadPoolSize;
        this.authorizationEnabled = authorizationEnabled;
        this.scheme = scheme;
        this.user = user;
        this.password = password;
    }
}
