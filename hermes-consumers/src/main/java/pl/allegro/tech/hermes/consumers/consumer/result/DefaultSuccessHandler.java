package pl.allegro.tech.hermes.consumers.consumer.result;

import static pl.allegro.tech.hermes.consumers.consumer.message.MessageConverter.toMessageMetadata;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.api.subscription.metrics.SubscriptionMetricsConfig;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.metrics.HermesCounter;
import pl.allegro.tech.hermes.metrics.HermesHistogram;
import pl.allegro.tech.hermes.metrics.HermesTimer;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

public class DefaultSuccessHandler implements SuccessHandler, SubscriptionChangeListener {

  private static final Logger logger = LoggerFactory.getLogger(DefaultSuccessHandler.class);

  private final Trackers trackers;
  private final SubscriptionName subscriptionName;
  private final MetricsFacade metrics;
  private final Map<Integer, HermesCounter> httpStatusCodes = new ConcurrentHashMap<>();
  private final HermesCounter throughputInBytes;
  private final HermesCounter successes;
  private final HermesHistogram inflightTime;
  private volatile HermesTimer messageProcessingTime;

  public DefaultSuccessHandler(
      MetricsFacade metrics,
      Trackers trackers,
      SubscriptionName subscriptionName,
      SubscriptionMetricsConfig metricsConfig) {
    this.metrics = metrics;
    this.trackers = trackers;
    this.subscriptionName = subscriptionName;
    this.throughputInBytes = metrics.subscriptions().throughputInBytes(subscriptionName);
    this.successes = metrics.subscriptions().successes(subscriptionName);
    this.inflightTime = metrics.subscriptions().inflightTimeInMillisHistogram(subscriptionName);
    this.messageProcessingTime =
        this.metrics
            .subscriptions()
            .messageProcessingTimeInMillisHistogram(
                this.subscriptionName, metricsConfig.messageProcessingDuration());
  }

  @Override
  public void handleSuccess(
      Message message, Subscription subscription, MessageSendingResult result) {
    markSuccess(message, result);
    trackers
        .get(subscription)
        .logSent(toMessageMetadata(message, subscription), result.getHostname());
  }

  @Override
  public void updateSubscription(Subscription subscription) {
    logger.info(
        "Subscription {} updated. Metrics configuration: {}",
        subscription.getQualifiedName(),
        subscription.getMetricsConfig());
    this.messageProcessingTime =
        metrics
            .subscriptions()
            .messageProcessingTimeInMillisHistogram(
                this.subscriptionName, subscription.getMetricsConfig().messageProcessingDuration());
  }

  private void markSuccess(Message message, MessageSendingResult result) {
    successes.increment();
    throughputInBytes.increment(message.getSize());
    markHttpStatusCode(result.getStatusCode());
    inflightTime.record(System.currentTimeMillis() - message.getReadingTimestamp());
    markMessageProcessingTime(message);
  }

  private void markHttpStatusCode(int statusCode) {
    httpStatusCodes
        .computeIfAbsent(
            statusCode,
            integer -> metrics.subscriptions().httpAnswerCounter(subscriptionName, statusCode))
        .increment();
  }

  private void markMessageProcessingTime(Message message) {
    if (messageProcessingTime != null) {
      Duration processingTime =
          Duration.ofMillis(System.currentTimeMillis() - message.getPublishingTimestamp());
      messageProcessingTime.record(processingTime);
    }
  }
}
