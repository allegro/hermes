package pl.allegro.tech.hermes.common.message.tracker.mongo;

import com.codahale.metrics.Timer;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mongodb.DB;
import com.mongodb.DBObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class MongoQueueCommitter implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(MongoQueueCommitter.class);

    private final BlockingQueue<DBObject> queue;
    private final String targetCollection;
    private final DB database;
    private final Timer timer;

    public MongoQueueCommitter(BlockingQueue<DBObject> queue, String targetCollection, DB database, Timer timer) {
        this.queue = queue;
        this.targetCollection = targetCollection;
        this.database = database;
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
            LOGGER.error("Could not commit to mongo.", ex);
        }
    }

    private void commit() {
        List<DBObject> batch = new ArrayList<>();
        queue.drainTo(batch);
        database.getCollection(targetCollection).insert(batch);
    }

    public static void scheduleCommitAtFixedRate(BlockingQueue<DBObject> queue,
                                                 String targetCollection,
                                                 DB database,
                                                 Timer timer,
                                                 int interval) {

        MongoQueueCommitter committer = new MongoQueueCommitter(queue, targetCollection, database, timer);
        ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat("mongo-queue-committer-%d").build();
        newSingleThreadScheduledExecutor(factory).scheduleAtFixedRate(committer, interval, interval, MILLISECONDS);
    }
}
