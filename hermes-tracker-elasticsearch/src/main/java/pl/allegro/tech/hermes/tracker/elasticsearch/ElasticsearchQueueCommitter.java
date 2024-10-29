package pl.allegro.tech.hermes.tracker.elasticsearch;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadFactory;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentType;
import pl.allegro.tech.hermes.metrics.HermesTimer;
import pl.allegro.tech.hermes.tracker.QueueCommitter;

public class ElasticsearchQueueCommitter extends QueueCommitter<ElasticsearchDocument> {

  private final IndexFactory indexFactory;
  private final Client client;
  private final String typeName;

  public ElasticsearchQueueCommitter(
      BlockingQueue<ElasticsearchDocument> queue,
      HermesTimer timer,
      IndexFactory indexFactory,
      String typeName,
      Client client) {
    super(queue, timer);
    this.indexFactory = indexFactory;
    this.typeName = typeName;
    this.client = client;
  }

  @Override
  protected void processBatch(List<ElasticsearchDocument> batch)
      throws ExecutionException, InterruptedException {
    BulkRequestBuilder bulk = client.prepareBulk();
    batch.forEach(
        entry ->
            bulk.add(
                client
                    .prepareIndex(indexFactory.createIndex(), typeName)
                    .setSource(entry.bytes(), XContentType.JSON)));
    bulk.execute().get();
  }

  public static void scheduleCommitAtFixedRate(
      BlockingQueue<ElasticsearchDocument> queue,
      IndexFactory indexFactory,
      String typeName,
      Client client,
      HermesTimer timer,
      int interval) {
    ElasticsearchQueueCommitter committer =
        new ElasticsearchQueueCommitter(queue, timer, indexFactory, typeName, client);
    ThreadFactory factory =
        new ThreadFactoryBuilder().setNameFormat("elasticsearch-queue-committer-%d").build();
    newSingleThreadScheduledExecutor(factory)
        .scheduleAtFixedRate(committer, interval, interval, MILLISECONDS);
  }
}
