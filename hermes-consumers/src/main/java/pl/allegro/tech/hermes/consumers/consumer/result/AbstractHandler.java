package pl.allegro.tech.hermes.consumers.consumer.result;

import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.consumers.consumer.offset.SubscriptionOffsetCommitQueues;
import pl.allegro.tech.hermes.consumers.consumer.Message;

public abstract class AbstractHandler {
    protected SubscriptionOffsetCommitQueues offsetHelper;
    protected HermesMetrics hermesMetrics;

    public AbstractHandler(SubscriptionOffsetCommitQueues offsetHelper, HermesMetrics hermesMetrics) {
        this.offsetHelper = offsetHelper;
        this.hermesMetrics = hermesMetrics;
    }

    protected void updateMetrics(String counterToUpdate, Message message, Subscription subscription) {
        hermesMetrics.counter(counterToUpdate, subscription.getTopicName(), subscription.getName()).inc();
        hermesMetrics.decrementInflightCounter(subscription);
    }
}
