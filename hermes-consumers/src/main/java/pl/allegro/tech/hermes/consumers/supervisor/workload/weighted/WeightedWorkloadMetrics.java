package pl.allegro.tech.hermes.consumers.supervisor.workload.weighted;

import com.codahale.metrics.Gauge;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;

public class WeightedWorkloadMetrics {

    private static final String METRICS_PREFIX = "consumer-workload.weighted.";
    private static final String CONSUMER_ID_PLACEHOLDER = "$consumerId";
    private static final String CURRENT_SCORE = METRICS_PREFIX + CONSUMER_ID_PLACEHOLDER + ".current-score";
    private static final String PROPOSED_SCORE = METRICS_PREFIX + CONSUMER_ID_PLACEHOLDER + ".proposed-score";
    private static final String SCORING_ERROR = METRICS_PREFIX + CONSUMER_ID_PLACEHOLDER + ".error";
    private static final String CURRENT_WEIGHT_OPS = METRICS_PREFIX + CONSUMER_ID_PLACEHOLDER + ".current-weight.ops";
    private static final String PROPOSED_WEIGHT_OPS = METRICS_PREFIX + CONSUMER_ID_PLACEHOLDER + ".proposed-weight.ops";

    private final HermesMetrics metrics;

    private final Map<String, Double> currentWeights = new ConcurrentHashMap<>();
    private final Map<String, Double> proposedWeights = new ConcurrentHashMap<>();
    private final Map<String, Double> currentScores = new ConcurrentHashMap<>();
    private final Map<String, Double> proposedScores = new ConcurrentHashMap<>();
    private final Map<String, Double> scoringErrors = new ConcurrentHashMap<>();

    public WeightedWorkloadMetrics(HermesMetrics metrics) {
        this.metrics = metrics;
    }

    void reportCurrentScore(String consumerId, double score) {
        registerGaugeIfNeeded(currentScores, consumerId, CURRENT_SCORE);
        currentScores.put(consumerId, score);
    }

    void reportProposedScore(String consumerId, double score) {
        registerGaugeIfNeeded(proposedScores, consumerId, PROPOSED_SCORE);
        proposedScores.put(consumerId, score);
    }

    void reportScoringError(String consumerId, double error) {
        registerGaugeIfNeeded(scoringErrors, consumerId, SCORING_ERROR);
        scoringErrors.put(consumerId, error);
    }

    void reportCurrentWeights(Collection<ConsumerNode> consumers) {
        for (ConsumerNode consumerNode : consumers) {
            String consumerId = consumerNode.getConsumerId();
            registerGaugeIfNeeded(currentWeights, consumerId, CURRENT_WEIGHT_OPS);
            currentWeights.put(consumerId, consumerNode.getWeight().getOperationsPerSecond());
        }
    }

    void reportProposedWeights(Map<String, Weight> newWeights) {
        for (Map.Entry<String, Weight> entry : newWeights.entrySet()) {
            String consumerId = entry.getKey();
            registerGaugeIfNeeded(proposedWeights, consumerId, PROPOSED_WEIGHT_OPS);
            proposedWeights.put(consumerId, entry.getValue().getOperationsPerSecond());
        }
    }

    private void registerGaugeIfNeeded(Map<String, Double> currentValues, String consumerId, String metric) {
        if (!currentValues.containsKey(consumerId)) {
            String metricPath = buildFullMetricPath(metric, consumerId);
            metrics.registerGauge(metricPath, (Gauge<Double>) () -> currentValues.getOrDefault(consumerId, 0d));
        }
    }

    void unregisterLeaderMetrics() {
        unregisterMetricsForConsumersOtherThan(emptySet());
    }

    void unregisterMetricsForConsumersOtherThan(Set<String> consumerIds) {
        unregisterGaugesForConsumersOtherThan(currentWeights, consumerIds, CURRENT_WEIGHT_OPS);
        unregisterGaugesForConsumersOtherThan(proposedWeights, consumerIds, PROPOSED_WEIGHT_OPS);
        unregisterGaugesForConsumersOtherThan(currentScores, consumerIds, CURRENT_SCORE);
        unregisterGaugesForConsumersOtherThan(proposedScores, consumerIds, PROPOSED_SCORE);
        unregisterGaugesForConsumersOtherThan(scoringErrors, consumerIds, SCORING_ERROR);
    }

    private void unregisterGaugesForConsumersOtherThan(Map<String, Double> currentValues, Set<String> consumerIds, String metric) {
        Set<String> consumerIdsToRemove = currentValues.keySet().stream()
                .filter(consumerId -> !consumerIds.contains(consumerId))
                .collect(toSet());
        for (String consumerId : consumerIdsToRemove) {
            if (!consumerIds.contains(consumerId)) {
                String metricPath = buildFullMetricPath(metric, consumerId);
                metrics.unregister(metricPath);
                currentValues.remove(consumerId);
            }
        }
    }

    private String buildFullMetricPath(String metric, String consumerId) {
        return metric.replace(CONSUMER_ID_PLACEHOLDER, HermesMetrics.escapeDots(consumerId));
    }
}
