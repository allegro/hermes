package pl.allegro.tech.hermes.tracker.elasticsearch.management;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static pl.allegro.tech.hermes.tracker.elasticsearch.SchemaManager.schemaManagerWithDailyIndexes;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import pl.allegro.tech.hermes.api.MessageTrace;
import pl.allegro.tech.hermes.api.PublishedMessageTrace;
import pl.allegro.tech.hermes.api.SentMessageTrace;
import pl.allegro.tech.hermes.api.SentMessageTraceStatus;
import pl.allegro.tech.hermes.tracker.elasticsearch.ElasticsearchRepositoryException;
import pl.allegro.tech.hermes.tracker.elasticsearch.LogSchemaAware;
import pl.allegro.tech.hermes.tracker.elasticsearch.SchemaManager;
import pl.allegro.tech.hermes.tracker.management.LogRepository;

public class ElasticsearchLogRepository implements LogRepository, LogSchemaAware {

  private static final int LIMIT = 1000;

  private final Client elasticClient;

  private final ObjectMapper objectMapper = new ObjectMapper();

  public ElasticsearchLogRepository(Client elasticClient) {
    this(elasticClient, schemaManagerWithDailyIndexes(elasticClient));
  }

  public ElasticsearchLogRepository(Client elasticClient, SchemaManager schemaManager) {
    this.elasticClient = elasticClient;
    schemaManager.ensureSchema();
  }

  @Override
  public List<SentMessageTrace> getLastUndeliveredMessages(
      String topicName, String subscriptionName, int limit) {
    SearchResponse response =
        searchSentMessages(
            limit,
            SortOrder.DESC,
            boolQuery()
                .must(termQuery(TOPIC_NAME, topicName))
                .must(termQuery(SUBSCRIPTION, subscriptionName))
                .must(termQuery(STATUS, SentMessageTraceStatus.DISCARDED.name())));

    return stream(response.getHits().getHits())
        .map(hit -> toMessageTrace(hit, SentMessageTrace.class))
        .collect(toList());
  }

  @Override
  public List<MessageTrace> getMessageStatus(
      String topicName, String subscriptionName, String messageId) {

    try {
      SearchResponse publishedResponse =
          searchPublishedMessages(
              LIMIT,
              boolQuery()
                  .must(termQuery(TOPIC_NAME, topicName))
                  .must(termQuery(MESSAGE_ID, messageId)));

      SearchResponse sentResponse =
          searchSentMessages(
              LIMIT,
              SortOrder.ASC,
              boolQuery()
                  .must(termQuery(TOPIC_NAME, topicName))
                  .must(termQuery(SUBSCRIPTION, subscriptionName))
                  .must(termQuery(MESSAGE_ID, messageId)));

      return Stream.concat(
              stream(publishedResponse.getHits().getHits())
                  .map(hit -> toMessageTrace(hit, PublishedMessageTrace.class)),
              stream(sentResponse.getHits().getHits())
                  .map(hit -> toMessageTrace(hit, SentMessageTrace.class)))
          .collect(toList());
    } catch (InterruptedException | ExecutionException ex) {
      throw new ElasticsearchRepositoryException(ex);
    }
  }

  private SearchResponse searchSentMessages(int limit, SortOrder sort, QueryBuilder query) {
    return elasticClient
        .prepareSearch(SchemaManager.SENT_ALIAS_NAME)
        .setTypes(SchemaManager.SENT_TYPE)
        .setTrackScores(true)
        .setQuery(query)
        .addSort(TIMESTAMP_SECONDS, sort)
        .setSize(limit)
        .execute()
        .actionGet();
  }

  private SearchResponse searchPublishedMessages(int limit, QueryBuilder query)
      throws InterruptedException, ExecutionException {
    return elasticClient
        .prepareSearch(SchemaManager.PUBLISHED_ALIAS_NAME)
        .setTypes(SchemaManager.PUBLISHED_TYPE)
        .setTrackScores(true)
        .setQuery(query)
        .addSort(TIMESTAMP_SECONDS, SortOrder.ASC)
        .setSize(limit)
        .execute()
        .get();
  }

  private <T> T toMessageTrace(SearchHit h, Class<T> messageTraceType) {
    try {
      return objectMapper.readValue(h.getSourceRef().streamInput(), messageTraceType);
    } catch (IOException e) {
      throw new RuntimeException(
          "Exception during deserialization of message trace class named "
              + messageTraceType.getCanonicalName(),
          e);
    }
  }
}
