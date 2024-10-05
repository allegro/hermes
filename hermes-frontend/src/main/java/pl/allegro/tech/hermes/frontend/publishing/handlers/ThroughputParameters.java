package pl.allegro.tech.hermes.frontend.publishing.handlers;

import java.time.Duration;

public interface ThroughputParameters {

  String getType();

  long getFixedMax();

  long getDynamicMax();

  long getDynamicThreshold();

  long getDynamicDesired();

  double getDynamicIdle();

  Duration getDynamicCheckInterval();
}
