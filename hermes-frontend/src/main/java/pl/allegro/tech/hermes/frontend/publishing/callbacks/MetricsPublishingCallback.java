package pl.allegro.tech.hermes.frontend.publishing.callbacks;

import com.codahale.metrics.Timer;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.metric.Counters;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.Meters;
import pl.allegro.tech.hermes.common.metric.Timers;
import pl.allegro.tech.hermes.frontend.publishing.Message;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;

public class MetricsPublishingCallback implements PublishingCallback {

    private final HermesMetrics hermesMetrics;
    private final Timer.Context latencyTimer;
    private final Timer.Context latencyTimerPerTopic;

    public MetricsPublishingCallback(HermesMetrics hermesMetrics, Topic topic) {
        this.hermesMetrics = hermesMetrics;
        latencyTimer = hermesMetrics.timer(Timers.PRODUCER_BROKER_LATENCY).time();
        latencyTimerPerTopic = hermesMetrics.timer(Timers.PRODUCER_BROKER_TOPIC_LATENCY, topic.getName()).time();
    }

    @Override
    public void onUnpublished(Exception exception) {
        HermesMetrics.close(latencyTimer, latencyTimerPerTopic);
    }

    @Override
    public void onPublished(Message message, Topic topic) {
        HermesMetrics.close(latencyTimer, latencyTimerPerTopic);
        hermesMetrics.meter(Meters.PRODUCER_METER).mark();
        hermesMetrics.meter(Meters.PRODUCER_TOPIC_METER, topic.getName()).mark();
        hermesMetrics.counter(Counters.PRODUCER_PUBLISHED, topic.getName()).inc();
    }
}
