package pl.allegro.tech.hermes.common.metric.timer;

import com.codahale.metrics.Timer;

import java.io.Closeable;

public class StartedTimersPair implements Closeable {

    private final Timer.Context time1;
    private final Timer.Context time2;

    public StartedTimersPair(Timer timer1, Timer timer2) {
        time1 = timer1.time();
        time2 = timer2.time();
    }

    @Override
    public void close() {
        time1.stop();
        time2.stop();
    }
}
