package pl.allegro.tech.hermes.common.di.factories;

import java.time.Duration;

public interface ZookeeperParameters {

  String getConnectionString();

  Duration getBaseSleepTime();

  Duration getMaxSleepTime();

  int getMaxRetries();

  Duration getConnectionTimeout();

  Duration getSessionTimeout();

  String getRoot();

  int getProcessingThreadPoolSize();

  boolean isAuthorizationEnabled();

  String getScheme();

  String getUser();

  String getPassword();
}
