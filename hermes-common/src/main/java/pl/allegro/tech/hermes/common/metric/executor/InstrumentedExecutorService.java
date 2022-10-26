package pl.allegro.tech.hermes.common.metric.executor;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import pl.allegro.tech.hermes.common.metric.HermesMetrics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * <p>This is a modified copy of InstrumentedExecutorService from
 * <a href="https://github.com/dropwizard/metrics">@dropwizard/metrics</a>.</p>
 *
 * <p>An {@link ExecutorService} that monitors the number of tasks submitted, running,
 * completed and also keeps a {@link Timer} for the task duration.</p>
 *
 * <p>It will register the metrics using the given (or auto-generated) name as classifier, e.g:
 * "your-executor-service.submitted", "your-executor-service.running", etc.</p>
 */
public class InstrumentedExecutorService implements ExecutorService {

    private final ExecutorService delegate;

    protected final Meter submitted;
    protected final Counter running;
    protected final Meter completed;
    protected final Timer duration;
    protected final Timer waiting;

    public InstrumentedExecutorService(ExecutorService delegate, HermesMetrics hermesMetrics, String name) {
        this.delegate = delegate;

        this.submitted = hermesMetrics.executorSubmittedMeter(name);
        this.running = hermesMetrics.executorRunningCounter(name);
        this.completed = hermesMetrics.executorCompletedMeter(name);
        this.duration = hermesMetrics.executorDurationTimer(name);
        this.waiting = hermesMetrics.executorWaitingTimer(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(Runnable runnable) {
        submitted.mark();
        Timer.Context waitingTimerContext = waiting.time();
        delegate.execute(new InstrumentedRunnable(runnable, waitingTimerContext));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Future<?> submit(Runnable runnable) {
        submitted.mark();
        Timer.Context waitingTimerContext = waiting.time();
        return delegate.submit(new InstrumentedRunnable(runnable, waitingTimerContext));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Future<T> submit(Runnable runnable, T result) {
        submitted.mark();
        Timer.Context waitingTimerContext = waiting.time();
        return delegate.submit(new InstrumentedRunnable(runnable, waitingTimerContext), result);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> Future<T> submit(Callable<T> task) {
        submitted.mark();
        Timer.Context waitingTimerContext = waiting.time();
        return delegate.submit(new InstrumentedCallable<T>(task, waitingTimerContext));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        submitted.mark(tasks.size());
        Collection<? extends Callable<T>> instrumented = instrument(tasks);
        return delegate.invokeAll(instrumented);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        submitted.mark(tasks.size());
        Collection<? extends Callable<T>> instrumented = instrument(tasks);
        return delegate.invokeAll(instrumented, timeout, unit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws ExecutionException, InterruptedException {
        submitted.mark(tasks.size());
        Collection<? extends Callable<T>> instrumented = instrument(tasks);
        return delegate.invokeAny(instrumented);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws ExecutionException, InterruptedException, TimeoutException {
        submitted.mark(tasks.size());
        Collection<? extends Callable<T>> instrumented = instrument(tasks);
        return delegate.invokeAny(instrumented, timeout, unit);
    }

    private <T> Collection<? extends Callable<T>> instrument(Collection<? extends Callable<T>> tasks) {
        final List<InstrumentedCallable<T>> instrumented = new ArrayList<InstrumentedCallable<T>>(tasks.size());
        for (Callable<T> task : tasks) {
            Timer.Context waitingTimerContext = waiting.time();
            instrumented.add(new InstrumentedCallable<T>(task, waitingTimerContext));
        }
        return instrumented;
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long l, TimeUnit timeUnit) throws InterruptedException {
        return delegate.awaitTermination(l, timeUnit);
    }

    protected class InstrumentedRunnable implements Runnable {
        private final Runnable task;
        private final Timer.Context waitingDurationTimerContext;

        InstrumentedRunnable(Runnable task, Timer.Context waitingDurationTimerContext) {
            this.task = task;
            this.waitingDurationTimerContext = waitingDurationTimerContext;
        }

        @Override
        public void run() {
            waitingDurationTimerContext.close();
            running.inc();
            final Timer.Context context = duration.time();
            try {
                task.run();
            } finally {
                context.stop();
                running.dec();
                completed.mark();
            }
        }
    }

    protected class InstrumentedCallable<T> implements Callable<T> {
        private final Callable<T> callable;
        private final Timer.Context waitingDurationTimerContext;

        InstrumentedCallable(Callable<T> callable, Timer.Context waitingDurationTimerContext) {
            this.callable = callable;
            this.waitingDurationTimerContext = waitingDurationTimerContext;
        }

        @Override
        public T call() throws Exception {
            waitingDurationTimerContext.close();
            running.inc();
            final Timer.Context context = duration.time();
            try {
                return callable.call();
            } finally {
                context.stop();
                running.dec();
                completed.mark();
            }
        }
    }
}
