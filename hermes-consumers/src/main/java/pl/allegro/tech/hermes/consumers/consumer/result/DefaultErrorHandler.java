package pl.allegro.tech.hermes.consumers.consumer.result;

import com.yammer.metrics.core.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.message.undelivered.UndeliveredMessageLog;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionOffsetCommitQueues;
import pl.allegro.tech.hermes.consumers.consumer.receiver.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.consumers.message.tracker.Trackers;

import static java.lang.String.format;
import static pl.allegro.tech.hermes.api.SentMessageTrace.createUndeliveredMessage;
import static pl.allegro.tech.hermes.common.metric.Metrics.Counter.CONSUMER_DISCARDED;
import static pl.allegro.tech.hermes.common.metric.Metrics.Meter.CONSUMER_DISCARDED_METER;
import static pl.allegro.tech.hermes.common.metric.Metrics.Meter.CONSUMER_FAILED_METER;

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
        LOGGER.info(format("Failed deliver message to endpoint %s; messageId %s; offset: %s; partition: %s; sub id: %s; rootCause: %s",
                        subscription.getEndpoint(), message.getId(), message.getOffset(), message.getPartition(), subscription.getId(),
                        result.getRootCause()), result.getFailure());

        offsetHelper.decrement(message.getPartition(), message.getOffset());
        updateMetrics(CONSUMER_DISCARDED, CONSUMER_DISCARDED_METER, message, subscription);
        undeliveredMessageLog.add(createUndeliveredMessage(subscription, new String(message.getData()), result.getFailure(), clock.time(),
                message.getPartition(), message.getOffset(), cluster));

        trackers.get(subscription).logDiscarded(message, subscription, result.getRootCause());
    }

    @Override
    public void handleFailed(Message message, Subscription subscription, MessageSendingResult result) {
        hermesMetrics.meter(CONSUMER_FAILED_METER, subscription.getTopicName(), subscription.getName()).mark();
        trackers.get(subscription).logFailed(message, subscription, result.getRootCause());
    }
}
