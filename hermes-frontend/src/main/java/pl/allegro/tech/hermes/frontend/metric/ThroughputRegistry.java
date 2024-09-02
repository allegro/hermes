package pl.allegro.tech.hermes.frontend.metric;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;

public class ThroughputRegistry {

  private final MetricsFacade metricsFacade;
  private final MetricRegistry metricRegistry;
  private final Meter globalThroughputMeter;

  public ThroughputRegistry(MetricsFacade metricsFacade, MetricRegistry metricRegistry) {
    this.metricsFacade = metricsFacade;
    this.metricRegistry = metricRegistry;
    this.globalThroughputMeter = metricRegistry.meter("globalThroughputMeter");
  }

  public double getGlobalThroughputOneMinuteRate() {
    return globalThroughputMeter.getOneMinuteRate();
  }

  public ThroughputMeter forTopic(TopicName topic) {
    return new ThroughputMeter(
        metricsFacade.topics().topicThroughputBytes(topic),
        metricsFacade.topics().topicGlobalThroughputBytes(),
        metricRegistry.meter(topic.qualifiedName() + "Throughput"),
        globalThroughputMeter);
  }
}
