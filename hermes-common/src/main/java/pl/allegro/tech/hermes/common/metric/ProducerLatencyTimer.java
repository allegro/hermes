package pl.allegro.tech.hermes.common.metric;

import com.codahale.metrics.Timer;

public abstract class ProducerLatencyTimer {

    protected final Timer.Context timer;
    protected final Timer.Context timerPerTopic;

    ProducerLatencyTimer(Timer timer, Timer timerPerTopic) {
        this.timer = timer.time();
        this.timerPerTopic = timerPerTopic.time();
    }

    public void close() {
        timer.close();
        timerPerTopic.close();
    }
}
