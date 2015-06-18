package pl.allegro.tech.hermes.tracker;

import com.codahale.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;

public abstract class QueueCommitter<T> implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(QueueCommitter.class);

    private final BlockingQueue<T> queue;
    private final Timer timer;

    public QueueCommitter(BlockingQueue<T> queue, Timer timer) {
        this.queue = queue;
        this.timer = timer;
    }

    @Override
    public void run() {
        try {
            if (!queue.isEmpty()) {
                Timer.Context ctx = timer.time();
                commit();
                ctx.close();
            }
        } catch (Exception ex) {
            LOGGER.error("Could not commit batch.", ex);
        }
    }

    private void commit() throws Exception {
        List<T> batch = new ArrayList<>();
        queue.drainTo(batch);
        processBatch(batch);
    }

    protected abstract void processBatch(List<T> batch) throws ExecutionException, InterruptedException;

}
