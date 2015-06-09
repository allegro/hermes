package pl.allegro.tech.hermes.message.tracker.mongo;

import com.mongodb.DBObject;

import java.util.concurrent.BlockingQueue;

public abstract class AbstractLogRepository {

    protected BlockingQueue<DBObject> queue;

    public AbstractLogRepository(BlockingQueue<DBObject> queue) {
        this.queue = queue;
    }

    public int getQueueSize() {
        return queue.size();
    }

    public int getQueueRemainingCapacity() {
        return queue.remainingCapacity();
    }

}
