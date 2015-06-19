package pl.allegro.tech.hermes.tracker.mongo;

import com.codahale.metrics.Timer;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mongodb.DB;
import com.mongodb.DBObject;
import pl.allegro.tech.hermes.tracker.QueueCommitter;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class MongoQueueCommitter extends QueueCommitter<DBObject> {

    private final String targetCollection;
    private final DB database;

    public MongoQueueCommitter(BlockingQueue<DBObject> queue, Timer timer, String targetCollection, DB database) {
        super(queue, timer);
        this.targetCollection = targetCollection;
        this.database = database;
    }

    @Override
    protected void processBatch(List<DBObject> batch) {
        database.getCollection(targetCollection).insert(batch);
    }

    public static void scheduleCommitAtFixedRate(BlockingQueue<DBObject> queue, String targetCollection, DB database, Timer timer, int interval) {
        MongoQueueCommitter committer = new MongoQueueCommitter(queue, timer, targetCollection, database);
        ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat("mongo-queue-committer-%d").build();
        newSingleThreadScheduledExecutor(factory).scheduleAtFixedRate(committer, interval, interval, MILLISECONDS);
    }
}
