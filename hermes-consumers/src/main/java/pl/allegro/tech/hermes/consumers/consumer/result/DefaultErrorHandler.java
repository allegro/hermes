package pl.allegro.tech.hermes.consumers.consumer.result;

import static pl.allegro.tech.hermes.api.SentMessageTrace.Builder.undeliveredMessage;
import static pl.allegro.tech.hermes.consumers.consumer.message.MessageConverter.toMessageMetadata;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.message.undelivered.UndeliveredMessageLog;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.metrics.HermesCounter;
import pl.allegro.tech.hermes.metrics.HermesHistogram;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

public class DefaultErrorHandler implements ErrorHandler {

  private static final Logger logger = LoggerFactory.getLogger(DefaultErrorHandler.class);

  private final MetricsFacade metrics;
  private final UndeliveredMessageLog undeliveredMessageLog;
  private final Clock clock;
  private final Trackers trackers;
  private final String cluster;
  private final SubscriptionName subscriptionName;
  private final HermesCounter failures;
  private final HermesCounter timeouts;
  private final HermesCounter otherErrors;
  private final HermesCounter discarded;
  private final HermesHistogram inflightTime;
  private final HermesCounter throughputInBytes;
  private final Map<Integer, HermesCounter> httpStatusCodes = new ConcurrentHashMap<>();

  public DefaultErrorHandler(
      MetricsFacade metrics,
      UndeliveredMessageLog undeliveredMessageLog,
      Clock clock,
      Trackers trackers,
      String cluster,
      SubscriptionName subscriptionName) {
    this.metrics = metrics;
    this.undeliveredMessageLog = undeliveredMessageLog;
    this.clock = clock;
    this.trackers = trackers;
    this.cluster = cluster;
    this.subscriptionName = subscriptionName;
    this.failures = metrics.subscriptions().failuresCounter(subscriptionName);
    this.timeouts = metrics.subscriptions().timeoutsCounter(subscriptionName);
    this.otherErrors = metrics.subscriptions().otherErrorsCounter(subscriptionName);
    this.discarded = metrics.subscriptions().discarded(subscriptionName);
    this.inflightTime = metrics.subscriptions().inflightTimeInMillisHistogram(subscriptionName);
    this.throughputInBytes = metrics.subscriptions().throughputInBytes(subscriptionName);
  }

  @Override
  public void handleDiscarded(
      Message message, Subscription subscription, MessageSendingResult result) {
    logResult(message, subscription, result);

    discarded.increment();
    inflightTime.record(System.currentTimeMillis() - message.getReadingTimestamp());

    addToMessageLog(message, subscription, result);

    trackers
        .get(subscription)
        .logDiscarded(toMessageMetadata(message, subscription), result.getRootCause());
  }

  private void addToMessageLog(
      Message message, Subscription subscription, MessageSendingResult result) {
    result
        .getLogInfo()
        .forEach(
            logInfo ->
                undeliveredMessageLog.add(
                    undeliveredMessage()
                        .withSubscription(subscription.getName())
                        .withTopicName(subscription.getQualifiedTopicName())
                        .withMessage(new String(message.getData()))
                        .withReason(logInfo.getFailure().getMessage())
                        .withTimestamp(clock.millis())
                        .withPartition(message.getPartition())
                        .withOffset(message.getOffset())
                        .withCluster(cluster)
                        .build()));
  }

  private void logResult(Message message, Subscription subscription, MessageSendingResult result) {
    if (result.isLoggable()) {
      result
          .getLogInfo()
          .forEach(
              logInfo ->
                  logger.warn(
                      "Abnormal delivery failure: "
                          + "subscription: {}; cause: {}; endpoint: {}; messageId: {}; partition: {}; offset: {}",
                      subscription.getQualifiedName(),
                      logInfo.getRootCause(),
                      logInfo.getUrlString(),
                      message.getId(),
                      message.getPartition(),
                      message.getOffset(),
                      logInfo.getFailure()));
    }
  }

  @Override
  public void handleFailed(
      Message message, Subscription subscription, MessageSendingResult result) {
    failures.increment();
    if (result.hasHttpAnswer()) {
      markHttpStatusCode(result.getStatusCode());
    } else if (result.isTimeout()) {
      timeouts.increment();
    } else {
      otherErrors.increment();
    }
    throughputInBytes.increment(message.getSize());
    trackers
        .get(subscription)
        .logFailed(
            toMessageMetadata(message, subscription), result.getRootCause(), result.getHostname());
  }

  private void markHttpStatusCode(int statusCode) {
    httpStatusCodes
        .computeIfAbsent(
            statusCode,
            integer -> metrics.subscriptions().httpAnswerCounter(subscriptionName, statusCode))
        .increment();
  }
}
