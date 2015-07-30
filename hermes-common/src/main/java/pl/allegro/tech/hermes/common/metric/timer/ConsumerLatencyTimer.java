package pl.allegro.tech.hermes.common.metric.timer;

import com.codahale.metrics.Timer;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;
import pl.allegro.tech.hermes.common.metric.Timers;

public class ConsumerLatencyTimer {

    private final Timer latencyTimer;
    private final Timer subscriptionLatencyTimer;

    public ConsumerLatencyTimer(HermesMetrics hermesMetrics, TopicName topicName, String subscriptionName) {
        latencyTimer = hermesMetrics.timer(Timers.CONSUMER_LATENCY);
        subscriptionLatencyTimer = hermesMetrics.timer(Timers.CONSUMER_SUBSCRIPTION_LATENCY, topicName, subscriptionName);
    }

    public Context time() {
        return new Context();
    }

    public class Context {

        private Timer.Context latencyTimerContext;
        private Timer.Context subscriptionLatencyTimerContext;

        private Context() {
            latencyTimerContext = latencyTimer.time();
            subscriptionLatencyTimerContext = subscriptionLatencyTimer.time();
        }

        public void stop() {
            latencyTimerContext.close();
            subscriptionLatencyTimerContext.close();
        }
    }
}
