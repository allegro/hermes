package pl.allegro.tech.hermes.common.metric.timer;

import com.codahale.metrics.Timer;

abstract class TopicAwareLatencyTimer {

    protected final Timer.Context timer;
    protected final Timer.Context timerPerTopic;

    TopicAwareLatencyTimer(Timer timer, Timer timerPerTopic) {
        this.timer = timer.time();
        this.timerPerTopic = timerPerTopic.time();
    }

    public void close() {
        timer.close();
        timerPerTopic.close();
    }
}
