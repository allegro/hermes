package pl.allegro.tech.hermes.consumers.consumer.rate.maxrate;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.consumers.consumer.rate.SendCounters;
import pl.allegro.tech.hermes.metrics.HermesCounter;

public class NegotiatedMaxRateProvider implements MaxRateProvider {

  private static final Logger logger = LoggerFactory.getLogger(NegotiatedMaxRateProvider.class);

  private final ConsumerInstance consumer;
  private final MaxRateRegistry registry;
  private final MaxRateSupervisor maxRateSupervisor;
  private final SendCounters sendCounters;
  private final MetricsFacade metrics;
  private final HermesCounter fetchFailuresCounter;
  private final HermesCounter historyUpdateFailuresCounter;
  private final double minSignificantChange;
  private final int historyLimit;
  private volatile double maxRate;
  private volatile double previousRecordedRate = -1;

  NegotiatedMaxRateProvider(
      String consumerId,
      MaxRateRegistry registry,
      MaxRateSupervisor maxRateSupervisor,
      Subscription subscription,
      SendCounters sendCounters,
      MetricsFacade metrics,
      double initialMaxRate,
      double minSignificantChange,
      int historyLimit) {
    this.consumer = new ConsumerInstance(consumerId, subscription.getQualifiedName());
    this.registry = registry;
    this.maxRateSupervisor = maxRateSupervisor;
    this.sendCounters = sendCounters;
    this.metrics = metrics;
    this.fetchFailuresCounter =
        metrics.maxRate().fetchFailuresCounter(subscription.getQualifiedName());
    this.historyUpdateFailuresCounter =
        metrics.maxRate().historyUpdateFailuresCounter(subscription.getQualifiedName());
    this.minSignificantChange = minSignificantChange;
    this.historyLimit = historyLimit;
    this.maxRate = initialMaxRate;
  }

  @Override
  public double get() {
    return maxRate;
  }

  void tickForHistory() {
    recordCurrentRate(sendCounters.getRate());
    fetchCurrentMaxRate().ifPresent(currentMaxRate -> maxRate = currentMaxRate.getMaxRate());
  }

  private void recordCurrentRate(double actualRate) {
    double usedRate = Math.min(actualRate / Math.max(maxRate, 1), 1.0d);
    if (shouldRecordHistory(usedRate)) {
      try {
        RateHistory rateHistory = registry.getRateHistory(consumer);
        RateHistory updatedHistory = RateHistory.updatedRates(rateHistory, usedRate, historyLimit);
        registry.writeRateHistory(consumer, updatedHistory);
        previousRecordedRate = usedRate;
      } catch (Exception e) {
        logger.warn("Encountered problem updating max rate for {}", consumer, e);
        historyUpdateFailuresCounter.increment();
      }
    }
  }

  private boolean shouldRecordHistory(double usedRate) {
    return previousRecordedRate < 0
        || Math.abs(previousRecordedRate - usedRate) > minSignificantChange;
  }

  private Optional<MaxRate> fetchCurrentMaxRate() {
    try {
      return registry.getMaxRate(consumer);
    } catch (Exception e) {
      logger.warn("Encountered problem fetching max rate for {}", consumer);
      fetchFailuresCounter.increment();
      return Optional.empty();
    }
  }

  public void start() {
    maxRateSupervisor.register(this);
    metrics
        .maxRate()
        .registerCalculatedRateGauge(
            consumer.getSubscription(), this, NegotiatedMaxRateProvider::get);
    metrics
        .maxRate()
        .registerActualRateGauge(consumer.getSubscription(), sendCounters, SendCounters::getRate);
  }

  public void shutdown() {
    maxRateSupervisor.unregister(this);
  }
}
