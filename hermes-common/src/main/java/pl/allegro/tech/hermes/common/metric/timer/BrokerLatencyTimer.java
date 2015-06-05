package pl.allegro.tech.hermes.common.metric.timer;

import com.codahale.metrics.Timer;

public abstract class BrokerLatencyTimer extends TopicAwareLatencyTimer {

    BrokerLatencyTimer(Timer timer, Timer timerPerTopic) {
        super(timer, timerPerTopic);
    }
}
