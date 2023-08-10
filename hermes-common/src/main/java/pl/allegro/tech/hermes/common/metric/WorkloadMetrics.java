package pl.allegro.tech.hermes.common.metric;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.search.Search;
import pl.allegro.tech.hermes.metrics.HermesTimer;

import java.util.Collection;
import java.util.Set;
import java.util.function.ToDoubleFunction;

public class WorkloadMetrics {

    private static final String CONSUMER_ID_TAG = "consumer-id";
    private static final String KAFKA_CLUSTER_TAG = "kafka-cluster";
    private static final String METRICS_PREFIX = "consumer-workload.weighted.";
    private static final String CONSUMER_ID_PLACEHOLDER = "$consumerId";
    private static final String CURRENT_SCORE = METRICS_PREFIX + CONSUMER_ID_PLACEHOLDER + ".current-score";
    private static final String PROPOSED_SCORE = METRICS_PREFIX + CONSUMER_ID_PLACEHOLDER + ".proposed-score";
    private static final String SCORING_ERROR = METRICS_PREFIX + CONSUMER_ID_PLACEHOLDER + ".error";
    private static final String CURRENT_WEIGHT_OPS = METRICS_PREFIX + CONSUMER_ID_PLACEHOLDER + ".current-weight.ops";
    private static final String PROPOSED_WEIGHT_OPS = METRICS_PREFIX + CONSUMER_ID_PLACEHOLDER + ".proposed-weight.ops";

    private final HermesMetrics hermesMetrics;
    private final MeterRegistry meterRegistry;
    private final GaugeRegistrar gaugeRegistrar;

    WorkloadMetrics(HermesMetrics hermesMetrics, MeterRegistry meterRegistry) {
        this.hermesMetrics = hermesMetrics;
        this.meterRegistry = meterRegistry;
        this.gaugeRegistrar = new GaugeRegistrar(meterRegistry, hermesMetrics);
    }

    public <T> void registerAllAssignmentsGauge(T obj, String kafkaCluster, ToDoubleFunction<T> f) {
        gaugeRegistrar.registerGauge(
                "consumers-workload." + kafkaCluster + ".all-assignments",
                "workload.all-assignments",
                obj,
                f,
                Tags.of(KAFKA_CLUSTER_TAG, kafkaCluster)
        );
    }

    public <T> void registerMissingResourcesGauge(T obj, String kafkaCluster, ToDoubleFunction<T> f) {
        gaugeRegistrar.registerGauge(
                "consumers-workload." + kafkaCluster + ".missing-resources",
                "workload.missing-resources",
                obj,
                f,
                Tags.of(KAFKA_CLUSTER_TAG, kafkaCluster)
        );
    }

    public <T> void registerDeletedAssignmentsGauge(T obj, String kafkaCluster, ToDoubleFunction<T> f) {
        gaugeRegistrar.registerGauge(
                "consumers-workload." + kafkaCluster + ".deleted-assignments",
                "workload.deleted-assignments",
                obj,
                f,
                Tags.of(KAFKA_CLUSTER_TAG, kafkaCluster)
        );
    }

    public <T> void registerCreatedAssignmentsGauge(T obj, String kafkaCluster, ToDoubleFunction<T> f) {
        gaugeRegistrar.registerGauge(
                "consumers-workload." + kafkaCluster + ".created-assignments",
                "workload.created-assignments",
                obj,
                f,
                Tags.of(KAFKA_CLUSTER_TAG, kafkaCluster)
        );
    }

    public HermesTimer rebalanceDurationTimer(String kafkaCluster) {
        return HermesTimer.from(
                meterRegistry.timer("workload.rebalance-duration", Tags.of(KAFKA_CLUSTER_TAG, kafkaCluster)),
                hermesMetrics.consumersWorkloadRebalanceDurationTimer(kafkaCluster)
        );
    }

    public <T> void registerRunningSubscriptionsGauge(T obj, ToDoubleFunction<T> f) {
        gaugeRegistrar.registerGauge("consumers-workload.monitor.running", "workload.subscriptions.running", obj, f);
    }

    public <T> void registerAssignedSubscriptionsGauge(T obj, ToDoubleFunction<T> f) {
        gaugeRegistrar.registerGauge("consumers-workload.monitor.assigned", "workload.subscriptions.assigned", obj, f);
    }

    public <T> void registerMissingSubscriptionsGauge(T obj, ToDoubleFunction<T> f) {
        gaugeRegistrar.registerGauge("consumers-workload.monitor.missing", "workload.subscriptions.missing", obj, f);
    }

    public <T> void registerOversubscribedGauge(T obj, ToDoubleFunction<T> f) {
        gaugeRegistrar.registerGauge("consumers-workload.monitor.oversubscribed", "workload.subscriptions.oversubscribed", obj, f);
    }

    public <T> void registerOperationsPerSecondGauge(T obj, ToDoubleFunction<T> f) {
        gaugeRegistrar.registerGauge("consumer-workload.weighted.load.ops", "workload.weighted.ops", obj, f);
    }

    public <T> void registerCpuUtilizationGauge(T obj, ToDoubleFunction<T> f) {
        gaugeRegistrar.registerGauge("consumer-workload.weighted.load.cpu-utilization", "workload.weighted.cpu-utilization", obj, f);
    }

    public <T> void registerCurrentScoreGauge(String consumerId, T obj, ToDoubleFunction<T> f) {
        gaugeRegistrar.registerGauge(
                buildFullGraphiteMetricPath(CURRENT_SCORE, consumerId),
                "workload.weighted.current-score",
                obj,
                f,
                Tags.of(CONSUMER_ID_TAG, consumerId)
        );
    }

    public <T> void registerProposedErrorGauge(String consumerId, T obj, ToDoubleFunction<T> f) {
        gaugeRegistrar.registerGauge(
                buildFullGraphiteMetricPath(PROPOSED_SCORE, consumerId),
                "workload.weighted.proposed-error",
                obj,
                f,
                Tags.of(CONSUMER_ID_TAG, consumerId)
        );
    }

    public <T> void registerScoringErrorGauge(String consumerId, T obj, ToDoubleFunction<T> f) {
        gaugeRegistrar.registerGauge(
                buildFullGraphiteMetricPath(SCORING_ERROR, consumerId),
                "workload.weighted.scoring-error",
                obj,
                f,
                Tags.of(CONSUMER_ID_TAG, consumerId)
        );
    }

    public <T> void registerCurrentWeightGauge(String consumerId, T obj, ToDoubleFunction<T> f) {
        gaugeRegistrar.registerGauge(
                buildFullGraphiteMetricPath(CURRENT_WEIGHT_OPS, consumerId),
                "workload.weighted.current-weight.ops",
                obj,
                f,
                Tags.of(CONSUMER_ID_TAG, consumerId)
        );
    }

    public <T> void registerProposedWeightGauge(String consumerId, T obj, ToDoubleFunction<T> f) {
        gaugeRegistrar.registerGauge(
                buildFullGraphiteMetricPath(PROPOSED_WEIGHT_OPS, consumerId),
                "workload.weighted.proposed-weight.ops",
                obj,
                f,
                Tags.of(CONSUMER_ID_TAG, consumerId)
        );
    }

    public void unregisterAllWorkloadWeightedGaugesForConsumerIds(Set<String> consumerIds) {
        Collection<Gauge> gauges = Search.in(meterRegistry)
                .tag(CONSUMER_ID_TAG, consumerIds::contains)
                .name(s -> s.startsWith("workload.weighted"))
                .gauges();
        for (Gauge gauge : gauges) {
            meterRegistry.remove(gauge);
        }
        for (String consumerId : consumerIds) {
            hermesMetrics.unregister(buildFullGraphiteMetricPath(CURRENT_SCORE, consumerId));
            hermesMetrics.unregister(buildFullGraphiteMetricPath(PROPOSED_SCORE, consumerId));
            hermesMetrics.unregister(buildFullGraphiteMetricPath(SCORING_ERROR, consumerId));
            hermesMetrics.unregister(buildFullGraphiteMetricPath(CURRENT_WEIGHT_OPS, consumerId));
            hermesMetrics.unregister(buildFullGraphiteMetricPath(PROPOSED_WEIGHT_OPS, consumerId));
        }
    }

    private String buildFullGraphiteMetricPath(String metric, String consumerId) {
        return metric.replace(CONSUMER_ID_PLACEHOLDER, HermesMetrics.escapeDots(consumerId));
    }
}
