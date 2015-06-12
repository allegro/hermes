package pl.allegro.tech.hermes.consumers.consumer.result;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.message.undelivered.UndeliveredMessageLog;
import pl.allegro.tech.hermes.common.metric.Counters;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.Meters;
import pl.allegro.tech.hermes.common.time.Clock;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionOffsetCommitQueues;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.consumers.message.tracker.Trackers;

import static java.lang.String.format;
import static pl.allegro.tech.hermes.api.SentMessageTrace.createUndeliveredMessage;

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

        updateMeters(subscription);
        updateMetrics(Counters.CONSUMER_DISCARDED, message, subscription);

        undeliveredMessageLog.add(createUndeliveredMessage(subscription, new String(message.getData()), result.getFailure(), clock.getTime(),
                message.getPartition(), message.getOffset(), cluster));

        trackers.get(subscription).logDiscarded(message, subscription, result.getRootCause());
    }

    private void updateMeters(Subscription subscription) {
        hermesMetrics.meter(Meters.CONSUMER_DISCARDED_METER).mark();
        hermesMetrics.meter(Meters.CONSUMER_DISCARDED_TOPIC_METER, subscription.getTopicName()).mark();
        hermesMetrics.meter(Meters.CONSUMER_DISCARDED_SUBSCRIPTION_METER, subscription.getTopicName(), subscription.getName()).mark();
    }

    @Override
    public void handleFailed(Message message, Subscription subscription, MessageSendingResult result) {
        hermesMetrics.meter(Meters.CONSUMER_FAILED_METER, subscription.getTopicName(), subscription.getName()).mark();

        trackers.get(subscription).logFailed(message, subscription, result.getRootCause());
    }
}
