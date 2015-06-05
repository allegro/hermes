package pl.allegro.tech.hermes.common.metric.timer;

import com.codahale.metrics.Timer;

public abstract class ProducerLatencyTimer extends TopicAwareLatencyTimer {

    ProducerLatencyTimer(Timer timer, Timer timerPerTopic) {
        super(timer, timerPerTopic);
    }
}
