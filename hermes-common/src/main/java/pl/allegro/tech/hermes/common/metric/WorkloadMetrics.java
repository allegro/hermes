package pl.allegro.tech.hermes.common.metric;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.search.Search;
import java.util.Collection;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import pl.allegro.tech.hermes.metrics.HermesTimer;

public class WorkloadMetrics {

  private static final String CONSUMER_ID_TAG = "consumer-id";
  private static final String KAFKA_CLUSTER_TAG = "kafka-cluster";

  private final MeterRegistry meterRegistry;
  private final GaugeRegistrar gaugeRegistrar;

  WorkloadMetrics(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
    this.gaugeRegistrar = new GaugeRegistrar(meterRegistry);
  }

  public <T> void registerAllAssignmentsGauge(T obj, String kafkaCluster, ToDoubleFunction<T> f) {
    gaugeRegistrar.registerGauge(
        "workload.all-assignments", obj, f, Tags.of(KAFKA_CLUSTER_TAG, kafkaCluster));
  }

  public <T> void registerMissingResourcesGauge(T obj, String kafkaCluster, ToDoubleFunction<T> f) {
    gaugeRegistrar.registerGauge(
        "workload.missing-resources", obj, f, Tags.of(KAFKA_CLUSTER_TAG, kafkaCluster));
  }

  public <T> void registerDeletedAssignmentsGauge(
      T obj, String kafkaCluster, ToDoubleFunction<T> f) {
    gaugeRegistrar.registerGauge(
        "workload.deleted-assignments", obj, f, Tags.of(KAFKA_CLUSTER_TAG, kafkaCluster));
  }

  public <T> void registerCreatedAssignmentsGauge(
      T obj, String kafkaCluster, ToDoubleFunction<T> f) {
    gaugeRegistrar.registerGauge(
        "workload.created-assignments", obj, f, Tags.of(KAFKA_CLUSTER_TAG, kafkaCluster));
  }

  public HermesTimer rebalanceDurationTimer(String kafkaCluster) {
    return HermesTimer.from(
        meterRegistry.timer(
            "workload.rebalance-duration", Tags.of(KAFKA_CLUSTER_TAG, kafkaCluster)));
  }

  public <T> void registerRunningSubscriptionsGauge(T obj, ToDoubleFunction<T> f) {
    gaugeRegistrar.registerGauge("workload.subscriptions.running", obj, f);
  }

  public <T> void registerAssignedSubscriptionsGauge(T obj, ToDoubleFunction<T> f) {
    gaugeRegistrar.registerGauge("workload.subscriptions.assigned", obj, f);
  }

  public <T> void registerMissingSubscriptionsGauge(T obj, ToDoubleFunction<T> f) {
    gaugeRegistrar.registerGauge("workload.subscriptions.missing", obj, f);
  }

  public <T> void registerOversubscribedGauge(T obj, ToDoubleFunction<T> f) {
    gaugeRegistrar.registerGauge("workload.subscriptions.oversubscribed", obj, f);
  }

  public <T> void registerOperationsPerSecondGauge(T obj, ToDoubleFunction<T> f) {
    gaugeRegistrar.registerGauge("workload.weighted.ops", obj, f);
  }

  public <T> void registerCpuUtilizationGauge(T obj, ToDoubleFunction<T> f) {
    gaugeRegistrar.registerGauge("workload.weighted.cpu-utilization", obj, f);
  }

  public <T> void registerCurrentScoreGauge(String consumerId, T obj, ToDoubleFunction<T> f) {
    gaugeRegistrar.registerGauge(
        "workload.weighted.current-score", obj, f, Tags.of(CONSUMER_ID_TAG, consumerId));
  }

  public <T> void registerProposedErrorGauge(String consumerId, T obj, ToDoubleFunction<T> f) {
    gaugeRegistrar.registerGauge(
        "workload.weighted.proposed-error", obj, f, Tags.of(CONSUMER_ID_TAG, consumerId));
  }

  public <T> void registerScoringErrorGauge(String consumerId, T obj, ToDoubleFunction<T> f) {
    gaugeRegistrar.registerGauge(
        "workload.weighted.scoring-error", obj, f, Tags.of(CONSUMER_ID_TAG, consumerId));
  }

  public <T> void registerCurrentWeightGauge(String consumerId, T obj, ToDoubleFunction<T> f) {
    gaugeRegistrar.registerGauge(
        "workload.weighted.current-weight.ops", obj, f, Tags.of(CONSUMER_ID_TAG, consumerId));
  }

  public <T> void registerProposedWeightGauge(String consumerId, T obj, ToDoubleFunction<T> f) {
    gaugeRegistrar.registerGauge(
        "workload.weighted.proposed-weight.ops", obj, f, Tags.of(CONSUMER_ID_TAG, consumerId));
  }

  public void unregisterAllWorkloadWeightedGaugesForConsumerIds(Set<String> consumerIds) {
    Collection<Gauge> gauges =
        Search.in(meterRegistry)
            .tag(CONSUMER_ID_TAG, consumerIds::contains)
            .name(s -> s.startsWith("workload.weighted"))
            .gauges();
    for (Gauge gauge : gauges) {
      meterRegistry.remove(gauge);
    }
  }
}
