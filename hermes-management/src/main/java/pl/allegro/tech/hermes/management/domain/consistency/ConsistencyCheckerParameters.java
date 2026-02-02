package pl.allegro.tech.hermes.management.domain.consistency;

import java.time.Duration;

public interface ConsistencyCheckerParameters {

  int getThreadPoolSize();

  boolean isPeriodicCheckEnabled();

  Duration getRefreshInterval();

  Duration getInitialRefreshDelay();
}
