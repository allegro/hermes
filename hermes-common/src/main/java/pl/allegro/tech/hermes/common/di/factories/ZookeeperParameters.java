package pl.allegro.tech.hermes.common.di.factories;

import java.time.Duration;

public interface ZookeeperParameters {

  String getConnectionString();

  String getDatacenter();

  Duration getBaseSleepTime();

  Duration getMaxSleepTime();

  int getMaxRetries();

  Duration getConnectionTimeout();

  Duration getSessionTimeout();

  String getRoot();

  int getProcessingThreadPoolSize();

  boolean isAuthenticationEnabled();

  String getScheme();

  String getUser();

  String getPassword();
}
