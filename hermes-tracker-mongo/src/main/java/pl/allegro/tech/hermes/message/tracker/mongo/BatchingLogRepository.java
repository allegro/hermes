package pl.allegro.tech.hermes.message.tracker.mongo;

import java.util.concurrent.BlockingQueue;

public abstract class BatchingLogRepository<T> {

    protected BlockingQueue<T> queue;

    public BatchingLogRepository(BlockingQueue<T> queue) {
        this.queue = queue;
    }

    public int getQueueSize() {
        return queue.size();
    }

    public int getQueueRemainingCapacity() {
        return queue.remainingCapacity();
    }

}
