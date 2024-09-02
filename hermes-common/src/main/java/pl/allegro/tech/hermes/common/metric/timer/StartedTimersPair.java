package pl.allegro.tech.hermes.common.metric.timer;

import java.io.Closeable;
import pl.allegro.tech.hermes.metrics.HermesTimerContext;

public class StartedTimersPair implements Closeable {

  private final HermesTimerContext time1;
  private final HermesTimerContext time2;

  public StartedTimersPair(HermesTimerContext timer1, HermesTimerContext timer2) {
    time1 = timer1;
    time2 = timer2;
  }

  @Override
  public void close() {
    time1.close();
    time2.close();
  }
}
