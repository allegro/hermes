package pl.allegro.tech.hermes.frontend.publishing.callbacks;

import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.metric.Counters;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.Meters;
import pl.allegro.tech.hermes.common.metric.timer.BrokerAckAllLatencyTimer;
import pl.allegro.tech.hermes.common.metric.timer.BrokerAckLeaderLatencyTimer;
import pl.allegro.tech.hermes.common.metric.timer.BrokerLatencyTimer;
import pl.allegro.tech.hermes.frontend.publishing.message.Message;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;

public class MetricsPublishingCallback implements PublishingCallback {

    private final HermesMetrics hermesMetrics;
    private final BrokerLatencyTimer brokerLatencyTimer;

    public MetricsPublishingCallback(HermesMetrics hermesMetrics, Topic topic) {
        this.hermesMetrics = hermesMetrics;
        this.brokerLatencyTimer = brokerLatencyTimer(topic);
    }

    private BrokerLatencyTimer brokerLatencyTimer(Topic topic) {
        if (Topic.Ack.ALL.equals(topic.getAck())) {
            return new BrokerAckAllLatencyTimer(hermesMetrics, topic.getName());
        } else {
            return new BrokerAckLeaderLatencyTimer(hermesMetrics, topic.getName());
        }
    }

    @Override
    public void onUnpublished(Message message, Topic topic, Exception exception) {
        brokerLatencyTimer.close();
    }

    @Override
    public void onPublished(Message message, Topic topic) {
        brokerLatencyTimer.close();
        hermesMetrics.meter(Meters.PRODUCER_METER).mark();
        hermesMetrics.meter(Meters.PRODUCER_TOPIC_METER, topic.getName()).mark();
        hermesMetrics.counter(Counters.PRODUCER_PUBLISHED, topic.getName()).inc();
    }
}
