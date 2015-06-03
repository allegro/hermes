package pl.allegro.tech.hermes.common.metric.executor;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class InstrumentedScheduledExecutorService extends InstrumentedExecutorService  implements ScheduledExecutorService {
    private final ScheduledExecutorService delegate;

    private final Counter scheduledOverrun;

    public InstrumentedScheduledExecutorService(ScheduledExecutorService delegate, HermesMetrics hermesMetrics, String name) {
        super(delegate, hermesMetrics, name);
        this.delegate = delegate;

        this.scheduledOverrun = hermesMetrics.scheduledExecutorOverrun(name);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        submitted.mark();
        Timer.Context waitingTimerContext = waiting.time();
        return delegate.schedule(new InstrumentedRunnable(command, waitingTimerContext), delay, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        submitted.mark();
        Timer.Context waitingTimerContext = waiting.time();
        return delegate.schedule(new InstrumentedCallable<>(callable, waitingTimerContext), delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return delegate.scheduleAtFixedRate(new InstrumentedPeriodicRunnable(command, period, unit), initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return delegate.scheduleWithFixedDelay(new InstrumentedPeriodicRunnable(command, delay, unit), initialDelay, delay, unit);
    }

    private class InstrumentedPeriodicRunnable implements Runnable {
        private final Runnable task;
        private final long periodInNanos;

        InstrumentedPeriodicRunnable(Runnable task, long period, TimeUnit unit) {
            periodInNanos = unit.toNanos(period);
            this.task = task;
        }

        @Override
        public void run() {
            running.inc();
            final Timer.Context context = duration.time();
            try {
                task.run();
            } finally {
                long elapsed = context.stop();
                running.dec();
                completed.mark();
                if (elapsed > periodInNanos) {
                    scheduledOverrun.inc();
                }
            }
        }
    }

}

