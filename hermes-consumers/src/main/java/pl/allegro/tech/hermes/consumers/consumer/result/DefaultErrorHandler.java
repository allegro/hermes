package pl.allegro.tech.hermes.consumers.consumer.result;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.message.undelivered.UndeliveredMessageLog;
import pl.allegro.tech.hermes.common.metric.Counters;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.Meters;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionOffsetCommitQueues;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import java.time.Clock;

import static pl.allegro.tech.hermes.api.SentMessageTrace.createUndeliveredMessage;
import static pl.allegro.tech.hermes.consumers.consumer.message.MessageConverter.toMessageMetadata;

public class DefaultErrorHandler extends AbstractHandler implements ErrorHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultErrorHandler.class);

    private final UndeliveredMessageLog undeliveredMessageLog;
    private final Clock clock;
    private final Trackers trackers;
    private final String cluster;

    public DefaultErrorHandler(SubscriptionOffsetCommitQueues offsetHelper, HermesMetrics hermesMetrics,
                               UndeliveredMessageLog undeliveredMessageLog, Clock clock, Trackers trackers, String cluster) {
        super(offsetHelper, hermesMetrics);
        this.undeliveredMessageLog = undeliveredMessageLog;
        this.clock = clock;
        this.trackers = trackers;
        this.cluster = cluster;
    }

    @Override
    public void handleDiscarded(Message message, Subscription subscription, MessageSendingResult result) {
        logResult(message, subscription, result);

        offsetHelper.remove(message);

        updateMeters(subscription);
        updateMetrics(Counters.DISCARDED, message, subscription);

        addToMessageLog(message, subscription, result);

        trackers.get(subscription).logDiscarded(toMessageMetadata(message, subscription), result.getRootCause());
    }

    private void addToMessageLog(Message message, Subscription subscription, MessageSendingResult result) {
        result.getLogInfo().forEach(logInfo ->
                undeliveredMessageLog.add(createUndeliveredMessage(subscription, new String(message.getData()), logInfo.getFailure(), clock.millis(),
                        message.getPartition(), message.getOffset(), cluster)));

    }

    private void logResult(Message message, Subscription subscription, MessageSendingResult result) {
        result.getLogInfo().forEach(logInfo ->
                LOGGER.warn(
                        "Abnormal delivery failure: subscription: {}; cause: {}; endpoint: {}; messageId: {}; partition: {}; offset: {}",
                        subscription.getId(), logInfo.getRootCause(), logInfo.getUrl(), message.getId(),
                        message.getPartition(), message.getOffset(), logInfo.getFailure()
                )
        );
    }

    private void updateMeters(Subscription subscription) {
        hermesMetrics.meter(Meters.DISCARDED_METER).mark();
        hermesMetrics.meter(Meters.DISCARDED_TOPIC_METER, subscription.getTopicName()).mark();
        hermesMetrics.meter(Meters.DISCARDED_SUBSCRIPTION_METER, subscription.getTopicName(), subscription.getName()).mark();
    }

    @Override
    public void handleFailed(Message message, Subscription subscription, MessageSendingResult result) {
        hermesMetrics.meter(Meters.FAILED_METER_SUBSCRIPTION, subscription.getTopicName(), subscription.getName()).mark();
        registerFailureMetrics(subscription, result);
        trackers.get(subscription).logFailed(toMessageMetadata(message, subscription), result.getRootCause());
    }

    private void registerFailureMetrics(Subscription subscription, MessageSendingResult result) {
        if (result.hasHttpAnswer()) {
            hermesMetrics.registerConsumerHttpAnswer(subscription, result.getStatusCode());
        }
        else if (result.isTimeout()) {
            hermesMetrics.consumerErrorsTimeoutMeter(subscription).mark();
        }
        else {
            hermesMetrics.consumerErrorsOtherMeter(subscription).mark();
        }
    }
}
