package pl.allegro.tech.hermes.consumers.consumer.result;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.message.undelivered.UndeliveredMessageLog;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.SubscriptionMetrics;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetQueue;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import java.time.Clock;

import static pl.allegro.tech.hermes.api.SentMessageTrace.Builder.undeliveredMessage;
import static pl.allegro.tech.hermes.consumers.consumer.message.MessageConverter.toMessageMetadata;
import static pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionPartitionOffset.subscriptionPartitionOffset;

public class DefaultErrorHandler implements ErrorHandler {

    private static final Logger logger = LoggerFactory.getLogger(DefaultErrorHandler.class);

    private final OffsetQueue offsetQueue;
    private final SubscriptionMetrics metrics;
    private final UndeliveredMessageLog undeliveredMessageLog;
    private final Clock clock;
    private final Trackers trackers;
    private final String cluster;

    public DefaultErrorHandler(OffsetQueue offsetQueue,
                               SubscriptionMetrics metrics,
                               UndeliveredMessageLog undeliveredMessageLog,
                               Clock clock,
                               Trackers trackers,
                               String cluster) {
        this.offsetQueue = offsetQueue;
        this.metrics = metrics;
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

        metrics.markDiscarded(message);

        addToMessageLog(message, subscription, result);

        trackers.get(subscription).logDiscarded(toMessageMetadata(message, subscription), result.getRootCause());
    }

    private void addToMessageLog(Message message, Subscription subscription, MessageSendingResult result) {
        result.getLogInfo().forEach(logInfo -> undeliveredMessageLog.add(
                undeliveredMessage()
                        .withSubscription(subscription.getName())
                        .withTopicName(subscription.getQualifiedTopicName())
                        .withMessage(new String(message.getData()))
                        .withReason(logInfo.getFailure().getMessage())
                        .withTimestamp(clock.millis())
                        .withPartition(message.getPartition())
                        .withOffset(message.getOffset())
                        .withCluster(cluster)
                        .build()
        ));
    }

    private void logResult(Message message, Subscription subscription, MessageSendingResult result) {
        if (result.isLoggable()) {
            result.getLogInfo().forEach(logInfo ->
                    logger.warn(
                            "Abnormal delivery failure: "
                                    + "subscription: {}; cause: {}; endpoint: {}; messageId: {}; partition: {}; offset: {}",
                            subscription.getQualifiedName(), logInfo.getRootCause(), logInfo.getUrlString(), message.getId(),
                            message.getPartition(), message.getOffset(), logInfo.getFailure()
                    )
            );
        }
    }

    @Override
    public void handleFailed(Message message, Subscription subscription, MessageSendingResult result) {
        metrics.markFailure(message, result);
        trackers.get(subscription).logFailed(toMessageMetadata(message, subscription), result.getRootCause(), result.getHostname());
    }
}
