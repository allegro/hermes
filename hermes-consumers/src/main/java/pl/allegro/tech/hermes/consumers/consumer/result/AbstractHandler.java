package pl.allegro.tech.hermes.consumers.consumer.result;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.offset.BetterOffsetQueue;
import pl.allegro.tech.hermes.consumers.consumer.Message;

public abstract class AbstractHandler {

    protected BetterOffsetQueue offsetQueue;
    protected HermesMetrics hermesMetrics;

    public AbstractHandler(BetterOffsetQueue offsetQueue, HermesMetrics hermesMetrics) {
        this.offsetQueue = offsetQueue;
        this.hermesMetrics = hermesMetrics;
    }

    protected void updateMetrics(String counterToUpdate, Message message, Subscription subscription) {
        hermesMetrics.counter(counterToUpdate, subscription.getTopicName(), subscription.getName()).inc();
        hermesMetrics.decrementInflightCounter(subscription);
        hermesMetrics.inflightTimeHistogram(subscription).update(System.currentTimeMillis() - message.getReadingTimestamp());
    }
}
