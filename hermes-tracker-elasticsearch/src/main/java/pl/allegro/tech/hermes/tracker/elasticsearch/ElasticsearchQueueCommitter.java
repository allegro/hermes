package pl.allegro.tech.hermes.tracker.elasticsearch;

import com.codahale.metrics.Timer;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import pl.allegro.tech.hermes.tracker.QueueCommitter;
import pl.allegro.tech.hermes.tracker.elasticsearch.LogSchemaAware.TypedIndex;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class ElasticsearchQueueCommitter extends QueueCommitter<XContentBuilder> {

    private final TypedIndex index;
    private final Client client;

    public ElasticsearchQueueCommitter(BlockingQueue<XContentBuilder> queue, Timer timer, TypedIndex index, Client client) {
        super(queue, timer);
        this.index = index;
        this.client = client;
    }

    @Override
    protected void processBatch(List<XContentBuilder> batch) throws ExecutionException, InterruptedException {
        BulkRequestBuilder bulk = client.prepareBulk();
        batch.forEach(entry -> bulk.add(client.prepareIndex(index.getIndex(), index.getType()).setSource(entry)));
        bulk.execute().get();
    }

    public static void scheduleCommitAtFixedRate(BlockingQueue<XContentBuilder> queue, TypedIndex index,
                                                 Client client, Timer timer, int interval) {
        ElasticsearchQueueCommitter committer = new ElasticsearchQueueCommitter(queue, timer, index, client);
        ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat("elasticsearch-queue-committer-%d").build();
        newSingleThreadScheduledExecutor(factory).scheduleAtFixedRate(committer, interval, interval, MILLISECONDS);
    }
}
