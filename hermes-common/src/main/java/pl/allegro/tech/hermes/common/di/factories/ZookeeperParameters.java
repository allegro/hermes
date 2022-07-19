package pl.allegro.tech.hermes.common.di.factories;

public interface ZookeeperParameters {

    String getConnectionString();

    int getBaseSleepTime();

    int getMaxSleepTimeSeconds();

    int getMaxRetries();

    int getConnectionTimeout();

    int getSessionTimeout();

    String getRoot();

    int getProcessingThreadPoolSize();

    boolean isAuthorizationEnabled();

    String getScheme();

    String getUser();

    String getPassword();
}
