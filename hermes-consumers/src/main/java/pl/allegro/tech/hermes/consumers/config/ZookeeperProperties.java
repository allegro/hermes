package pl.allegro.tech.hermes.consumers.config;

import pl.allegro.tech.hermes.common.di.factories.ZookeeperParameters;

public class ZookeeperProperties {

    private String connectionString = "localhost:2181";

    private String datacenter = "dc";

    private int baseSleepTime = 1000;

    private int maxSleepTimeSeconds = 30;

    private int maxRetries = 29;

    private int connectionTimeout = 10_000;

    private int sessionTimeout = 10_000;

    private String root = "/hermes";

    private int processingThreadPoolSize = 5;

    private ZookeeperAuthorizationProperties authorization = new ZookeeperAuthorizationProperties();

    public String getConnectionString() {
        return connectionString;
    }

    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }

    public String getDatacenter() {
        return datacenter;
    }

    public void setDatacenter(String datacenter) {
        this.datacenter = datacenter;
    }

    public int getBaseSleepTime() {
        return baseSleepTime;
    }

    public void setBaseSleepTime(int baseSleepTime) {
        this.baseSleepTime = baseSleepTime;
    }

    public int getMaxSleepTimeSeconds() {
        return maxSleepTimeSeconds;
    }

    public void setMaxSleepTimeSeconds(int maxSleepTimeSeconds) {
        this.maxSleepTimeSeconds = maxSleepTimeSeconds;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(int sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public int getProcessingThreadPoolSize() {
        return processingThreadPoolSize;
    }

    public void setProcessingThreadPoolSize(int processingThreadPoolSize) {
        this.processingThreadPoolSize = processingThreadPoolSize;
    }

    public ZookeeperAuthorizationProperties getAuthorization() {
        return authorization;
    }

    public void setAuthorization(ZookeeperAuthorizationProperties authorization) {
        this.authorization = authorization;
    }

    public static class ZookeeperAuthorizationProperties {

        private boolean enabled = false;

        private String scheme = "digest";

        private String user = "user";

        private String password = "password";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getScheme() {
            return scheme;
        }

        public void setScheme(String scheme) {
            this.scheme = scheme;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public ZookeeperParameters toZookeeperParameters() {
        return new ZookeeperParameters(
                this.connectionString,
                this.baseSleepTime,
                this.maxSleepTimeSeconds,
                this.maxRetries,
                this.connectionTimeout,
                this.sessionTimeout,
                this.root,
                this.processingThreadPoolSize,
                this.authorization.enabled,
                this.authorization.scheme,
                this.authorization.user,
                this.authorization.password
        );
    }
}
