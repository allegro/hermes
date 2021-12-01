package pl.allegro.tech.hermes.consumers.consumer.result;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.message.undelivered.UndeliveredMessageLog;
import pl.allegro.tech.hermes.common.metric.Counters;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.Meters;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetQueue;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.tracker.consumers.MessageMetadata;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import java.time.Clock;
import java.util.Map;

import static pl.allegro.tech.hermes.api.SentMessageTrace.createUndeliveredMessage;
import static pl.allegro.tech.hermes.common.http.ExtraRequestHeadersCollector.extraRequestHeadersCollector;
import static pl.allegro.tech.hermes.consumers.consumer.message.MessageConverter.toMessageMetadata;
import static pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset.subscriptionPartitionOffset;

public class DefaultErrorHandler extends AbstractHandler implements ErrorHandler {

    private static final Logger logger = LoggerFactory.getLogger(DefaultErrorHandler.class);

    private final UndeliveredMessageLog undeliveredMessageLog;
    private final Clock clock;
    private final Trackers trackers;
    private final String cluster;

    public DefaultErrorHandler(OffsetQueue offsetQueue,
                               HermesMetrics hermesMetrics,
                               UndeliveredMessageLog undeliveredMessageLog, Clock clock, Trackers trackers, String cluster) {
        super(offsetQueue, hermesMetrics);
        this.undeliveredMessageLog = undeliveredMessageLog;
        this.clock = clock;
        this.trackers = trackers;
        this.cluster = cluster;
    }

    @Override
    public void handleDiscarded(Message message, Subscription subscription, MessageSendingResult result) {
        logResult(message, subscription, result);

        offsetQueue.offerCommittedOffset(subscriptionPartitionOffset(subscription.getQualifiedName(),
                message.getPartitionOffset(), message.getPartitionAssignmentTerm()));

        updateMeters(subscription);
        updateMetrics(Counters.DISCARDED, message, subscription);

        MessageMetadata messageMetadata = toMessageMetadata(message, subscription);

        addToMessageLog(message, subscription, result, messageMetadata.getExtraRequestHeaders());

        trackers.get(subscription).logDiscarded(toMessageMetadata(message, subscription), result.getRootCause());
    }

    private void addToMessageLog(Message message, Subscription subscription, MessageSendingResult result, Map<String, String> extraRequestHeaders) {
        result.getLogInfo().forEach(logInfo ->
                undeliveredMessageLog.add(createUndeliveredMessage(subscription, new String(message.getData()), logInfo.getFailure(), clock.millis(),
                        message.getPartition(), message.getOffset(), cluster, extraRequestHeaders.entrySet().stream().collect(extraRequestHeadersCollector()))));

    }

    private void logResult(Message message, Subscription subscription, MessageSendingResult result) {
        if(result.isLoggable()) {
            result.getLogInfo().stream().forEach(logInfo ->
                    logger.warn(
                            "Abnormal delivery failure: subscription: {}; cause: {}; endpoint: {}; messageId: {}; partition: {}; offset: {}",
                            subscription.getQualifiedName(), logInfo.getRootCause(), logInfo.getUrlString(), message.getId(),
                            message.getPartition(), message.getOffset(), logInfo.getFailure()
                    )
            );
        }
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
        hermesMetrics.meter(
                Meters.SUBSCRIPTION_THROUGHPUT_BYTES,
                subscription.getTopicName(),
                subscription.getName())
                .mark(message.getSize());
        trackers.get(subscription).logFailed(toMessageMetadata(message, subscription), result.getRootCause(), result.getHostname());
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
