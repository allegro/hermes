package pl.allegro.tech.hermes.common.metric.timer;

import com.codahale.metrics.Timer;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.Timers;

public class ConsumerLatencyTimer {

    private Timer.Context latencyTimer;
    private Timer.Context subscriptionLatencyTimer;

    public ConsumerLatencyTimer(HermesMetrics hermesMetrics, TopicName topicName, String subscriptionName) {
        latencyTimer = hermesMetrics.timer(Timers.CONSUMER_LATENCY).time();
        subscriptionLatencyTimer = hermesMetrics.timer(Timers.CONSUMER_SUBSCRIPTION_LATENCY, topicName, subscriptionName).time();
    }

    public void stop() {
        latencyTimer.close();
        subscriptionLatencyTimer.close();
    }
}
