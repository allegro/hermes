package pl.allegro.tech.hermes.consumers.consumer.result;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.api.SubscriptionName;
import pl.allegro.tech.hermes.common.metric.MetricsFacade;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSendingResult;
import pl.allegro.tech.hermes.metrics.HermesCounter;
import pl.allegro.tech.hermes.metrics.HermesHistogram;
import pl.allegro.tech.hermes.tracker.consumers.Trackers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static pl.allegro.tech.hermes.consumers.consumer.message.MessageConverter.toMessageMetadata;

public class DefaultSuccessHandler implements SuccessHandler {

    private final Trackers trackers;
    private final SubscriptionName subscriptionName;
    private final MetricsFacade metrics;
    private final Map<Integer, HermesCounter> httpStatusCodes = new ConcurrentHashMap<>();
    private final HermesCounter throughputInBytes;
    private final HermesCounter successes;
    private final HermesHistogram inflightTime;

    public DefaultSuccessHandler(MetricsFacade metrics,
                                 Trackers trackers,
                                 SubscriptionName subscriptionName) {
        this.metrics = metrics;
        this.trackers = trackers;
        this.subscriptionName = subscriptionName;
        this.throughputInBytes = metrics.subscriptions().throughputInBytes(subscriptionName);
        this.successes = metrics.subscriptions().successes(subscriptionName);
        this.inflightTime = metrics.subscriptions().inflightTimeInMillisHistogram(subscriptionName);
    }

    @Override
    public void handleSuccess(Message message, Subscription subscription, MessageSendingResult result) {
        markSuccess(message, result);
        trackers.get(subscription).logSent(toMessageMetadata(message, subscription), result.getHostname());
    }

    private void markSuccess(Message message, MessageSendingResult result) {
        successes.increment();
        throughputInBytes.increment(message.getSize());
        markHttpStatusCode(result.getStatusCode());
        inflightTime.record(System.currentTimeMillis() - message.getReadingTimestamp());
    }

    private void markHttpStatusCode(int statusCode) {
        httpStatusCodes.computeIfAbsent(
                statusCode,
                integer -> metrics.subscriptions().httpAnswerCounter(subscriptionName, statusCode)
        ).increment();
    }
}
