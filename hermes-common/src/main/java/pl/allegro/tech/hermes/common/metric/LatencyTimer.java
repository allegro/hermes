package pl.allegro.tech.hermes.common.metric;

import com.codahale.metrics.Timer;
import pl.allegro.tech.hermes.api.TopicName;

public class LatencyTimer {

    private Timer.Context latencyTimer;
    private Timer.Context subscriptionLatencyTimer;

    LatencyTimer(HermesMetrics hermesMetrics, TopicName topicName, String subscriptionName) {
        latencyTimer = hermesMetrics.timer(Metrics.Timer.CONSUMER_LATENCY).time();
        subscriptionLatencyTimer = hermesMetrics.timer(Metrics.Timer.CONSUMER_LATENCY, topicName, subscriptionName).time();
    }

    public void stop() {
        latencyTimer.close();
        subscriptionLatencyTimer.close();
    }
}
