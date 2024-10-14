package pl.allegro.tech.hermes.test.helper.cache;

import com.google.common.base.Ticker;
import java.time.Duration;

public class FakeTicker extends Ticker {
  private long currentNanos = 0;

  @Override
  public long read() {
    return currentNanos;
  }

  public void advance(Duration duration) {
    currentNanos += duration.toNanos();
  }
}
