package pl.allegro.tech.hermes.common.clock;

import java.time.Clock;

public class ClockFactory {

  public Clock provide() {
    return Clock.systemDefaultZone();
  }
}
