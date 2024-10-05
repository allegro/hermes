package pl.allegro.tech.hermes.consumers.consumer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.consumers.consumer.batch.MessageBatch;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.metrics.HermesCounter;
import pl.allegro.tech.hermes.metrics.HermesHistogram;
import pl.allegro.tech.hermes.metrics.HermesTimer;

class BatchConsumerMetrics {

  private final MetricsFacade metrics;
  private final SubscriptionName subscriptionName;
  private final LongAdder inflightCount = new LongAdder();
  private final HermesCounter failures;
  private final HermesCounter timeouts;
  private final HermesCounter otherErrors;
  private final HermesCounter discarded;
  private final HermesHistogram inflightTime;
  private final HermesCounter throughputInBytes;
  private final HermesCounter successes;
  private final HermesCounter batchSuccesses;
  private final HermesTimer latency;
  private final Map<Integer, HermesCounter> httpStatusCodes = new ConcurrentHashMap<>();

  BatchConsumerMetrics(MetricsFacade metrics, SubscriptionName subscriptionName) {
    this.metrics = metrics;
    this.subscriptionName = subscriptionName;
    this.failures = metrics.subscriptions().failuresCounter(subscriptionName);
    this.timeouts = metrics.subscriptions().timeoutsCounter(subscriptionName);
    this.otherErrors = metrics.subscriptions().otherErrorsCounter(subscriptionName);
    this.discarded = metrics.subscriptions().discarded(subscriptionName);
    this.inflightTime = metrics.subscriptions().inflightTimeInMillisHistogram(subscriptionName);
    this.throughputInBytes = metrics.subscriptions().throughputInBytes(subscriptionName);
    this.successes = metrics.subscriptions().successes(subscriptionName);
    this.batchSuccesses = metrics.subscriptions().batchSuccesses(subscriptionName);
    this.latency = metrics.subscriptions().latency(subscriptionName);
  }

  void recordAttempt(int messageCount) {
    inflightCount.add(messageCount);
  }

  void recordAttemptAsFinished(int messageCount) {
    inflightCount.add(-1 * messageCount);
  }

  void markFailure(MessageBatch batch, MessageSendingResult result) {
    failures.increment();
    if (result.hasHttpAnswer()) {
      markHttpStatusCode(result.getStatusCode());
    } else if (result.isTimeout()) {
      timeouts.increment();
    } else {
      otherErrors.increment();
    }
    throughputInBytes.increment(batch.getSize());
  }

  void markSuccess(MessageBatch batch, MessageSendingResult result) {
    successes.increment(batch.getMessageCount());
    batchSuccesses.increment();
    throughputInBytes.increment(batch.getSize());
    markHttpStatusCode(result.getStatusCode());
    inflightTime.record(batch.getLifetime());
  }

  private void markHttpStatusCode(int statusCode) {
    httpStatusCodes
        .computeIfAbsent(
            statusCode,
            integer -> metrics.subscriptions().httpAnswerCounter(subscriptionName, statusCode))
        .increment();
  }

  void shutdown() {
    metrics.unregisterAllMetricsRelatedTo(subscriptionName);
  }

  void initialize() {
    metrics
        .subscriptions()
        .registerInflightGauge(
            subscriptionName, this, metrics -> metrics.inflightCount.doubleValue());
  }

  void markDiscarded() {
    discarded.increment();
  }

  void markDiscarded(MessageBatch batch) {
    discarded.increment(batch.getMessageCount());
    inflightTime.record(batch.getLifetime());
  }

  HermesTimer latencyTimer() {
    return latency;
  }
}
