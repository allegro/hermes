package pl.allegro.tech.hermes.tracker.elasticsearch;

import com.codahale.metrics.Timer;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import pl.allegro.tech.hermes.tracker.QueueCommitter;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class ElasticsearchQueueCommitter extends QueueCommitter<XContentBuilder> {

    private final String index;
    private final String type;
    private final Client client;

    public ElasticsearchQueueCommitter(BlockingQueue<XContentBuilder> queue, Timer timer, String index, String type, Client client) {
        super(queue, timer);
        this.index = index;
        this.type = type;
        this.client = client;
    }

    @Override
    protected void processBatch(List<XContentBuilder> batch) throws ExecutionException, InterruptedException {
        BulkRequestBuilder bulk = client.prepareBulk();
        batch.forEach(entry -> bulk.add(client.prepareIndex(index, type).setSource(entry)));
        bulk.execute().get();
    }

    public static void scheduleCommitAtFixedRate(BlockingQueue<XContentBuilder> queue, String index, String type,
                                                 Client client, Timer timer, int interval) {
        ElasticsearchQueueCommitter committer = new ElasticsearchQueueCommitter(queue, timer, index, type, client);
        ThreadFactory factory = new ThreadFactoryBuilder().setNameFormat("elasticsearch-queue-committer-%d").build();
        newSingleThreadScheduledExecutor(factory).scheduleAtFixedRate(committer, interval, interval, MILLISECONDS);
    }
}
