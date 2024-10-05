package pl.allegro.tech.hermes.frontend.metric;

import com.codahale.metrics.Meter;
import pl.allegro.tech.hermes.metrics.HermesCounter;
import pl.allegro.tech.hermes.metrics.HermesRateMeter;

public class ThroughputMeter implements HermesRateMeter {

  private final HermesCounter topicThroughputMetric;
  private final HermesCounter globalThroughputMetric;
  private final Meter topicMeter;
  private final Meter globalMeter;

  public ThroughputMeter(
      HermesCounter topicThroughputMetric,
      HermesCounter globalThroughputMetric,
      Meter topicMeter,
      Meter globalMeter) {
    this.topicThroughputMetric = topicThroughputMetric;
    this.globalThroughputMetric = globalThroughputMetric;
    this.topicMeter = topicMeter;
    this.globalMeter = globalMeter;
  }

  @Override
  public double getOneMinuteRate() {
    return topicMeter.getOneMinuteRate();
  }

  public void increment(long size) {
    this.topicMeter.mark(size);
    this.globalMeter.mark(size);
    this.topicThroughputMetric.increment(size);
    this.globalThroughputMetric.increment(size);
  }
}
