package pl.allegro.tech.hermes.consumers.consumer.result;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetCommitter;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetQueue;

public abstract class AbstractHandler {

    protected OffsetQueue offsetQueue;
    protected HermesMetrics hermesMetrics;

    public AbstractHandler(OffsetQueue offsetQueue, HermesMetrics hermesMetrics) {
        this.offsetQueue = offsetQueue;
        this.hermesMetrics = hermesMetrics;
    }

    protected void updateMetrics(String counterToUpdate, Message message, Subscription subscription) {
        hermesMetrics.counter(counterToUpdate, subscription.getTopicName(), subscription.getName()).inc();
        hermesMetrics.decrementInflightCounter(subscription);
        hermesMetrics.inflightTimeHistogram(subscription).update(System.currentTimeMillis() - message.getReadingTimestamp());
    }
}
