package pl.allegro.tech.hermes.frontend.publishing.callbacks;

import com.codahale.metrics.Timer;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.Metrics;
import pl.allegro.tech.hermes.frontend.publishing.Message;
import pl.allegro.tech.hermes.frontend.publishing.PublishingCallback;

public class MetricsPublishingCallback implements PublishingCallback {

    private final HermesMetrics hermesMetrics;
    private final Timer.Context latencyTimer;
    private final Timer.Context latencyTimerPerTopic;

    public MetricsPublishingCallback(HermesMetrics hermesMetrics, Topic topic) {
        this.hermesMetrics = hermesMetrics;
        latencyTimer = hermesMetrics.timer(Metrics.Timer.PRODUCER_BROKER_LATENCY).time();
        latencyTimerPerTopic = hermesMetrics.timer(Metrics.Timer.PRODUCER_BROKER_LATENCY, topic.getName()).time();
    }

    @Override
    public void onUnpublished(Exception exception) {
        HermesMetrics.close(latencyTimer, latencyTimerPerTopic);
    }

    @Override
    public void onPublished(Message message, Topic topic) {
        HermesMetrics.close(latencyTimer, latencyTimerPerTopic);
        hermesMetrics.meter(Metrics.Meter.PRODUCER_METER).mark();
        hermesMetrics.meter(Metrics.Meter.PRODUCER_METER, topic.getName()).mark();
        hermesMetrics.counter(Metrics.Counter.PRODUCER_PUBLISHED, topic.getName()).inc();
    }
}
