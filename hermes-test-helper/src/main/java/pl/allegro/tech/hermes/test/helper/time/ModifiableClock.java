package pl.allegro.tech.hermes.test.helper.time;

import static java.time.Instant.now;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public class ModifiableClock extends Clock {

  private Clock clock = fixed(now(systemDefaultZone()), ZoneId.systemDefault());

  public void advanceMinutes(int minutes) {
    clock = fixed(now(clock).plus(minutes, ChronoUnit.MINUTES), ZoneId.systemDefault());
  }

  public void advance(Duration step) {
    clock = fixed(now(clock).plus(step), ZoneId.systemDefault());
  }

  @Override
  public ZoneId getZone() {
    return clock.getZone();
  }

  @Override
  public Clock withZone(ZoneId zone) {
    return clock.withZone(zone);
  }

  @Override
  public Instant instant() {
    return clock.instant();
  }
}
