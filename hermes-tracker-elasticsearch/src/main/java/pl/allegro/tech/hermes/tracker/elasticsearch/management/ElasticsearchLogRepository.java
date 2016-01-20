package pl.allegro.tech.hermes.tracker.elasticsearch.management;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.FilteredQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import pl.allegro.tech.hermes.api.MessageTrace;
import pl.allegro.tech.hermes.api.PublishedMessageTrace;
import pl.allegro.tech.hermes.api.PublishedMessageTraceStatus;
import pl.allegro.tech.hermes.api.SentMessageTrace;
import pl.allegro.tech.hermes.api.SentMessageTraceStatus;
import pl.allegro.tech.hermes.tracker.elasticsearch.ElasticsearchRepositoryException;
import pl.allegro.tech.hermes.tracker.elasticsearch.LogSchemaAware;
import pl.allegro.tech.hermes.tracker.elasticsearch.SchemaManager;
import pl.allegro.tech.hermes.tracker.management.LogRepository;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.elasticsearch.index.query.QueryBuilders.filteredQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static pl.allegro.tech.hermes.tracker.elasticsearch.SchemaManager.schemaManagerWithDailyIndexes;

public class ElasticsearchLogRepository implements LogRepository, LogSchemaAware {

    private static final int LIMIT = 1000;

    private final Client elasticClient;

    public ElasticsearchLogRepository(Client elasticClient) {
        this(elasticClient, schemaManagerWithDailyIndexes(elasticClient));
    }

    public ElasticsearchLogRepository(Client elasticClient, SchemaManager schemaManager) {
        this.elasticClient = elasticClient;
        schemaManager.ensureSchema();
    }

    @Override
    public List<SentMessageTrace> getLastUndeliveredMessages(String topicName, String subscriptionName, int limit) {

        try {
            SearchResponse response = searchSentMessages(limit, SortOrder.DESC,
                    createFilterQuery(
                            filter(TOPIC_NAME, topicName),
                            filter(SUBSCRIPTION, subscriptionName),
                            filter(STATUS, SentMessageTraceStatus.DISCARDED.name())));

            return Arrays.stream(response.getHits().hits())
                    .map(this::toSentMessageTrace)
                    .collect(toList());

        } catch (InterruptedException | ExecutionException ex) {
            throw new ElasticsearchRepositoryException(ex);
        }
    }

    @Override
    public List<MessageTrace> getMessageStatus(String topicName, String subscriptionName, String messageId) {

        try {
            SearchResponse publishedResponse = searchPublishedMessages(LIMIT,
                    createFilterQuery(
                            filter(TOPIC_NAME, topicName),
                            filter(MESSAGE_ID, messageId)));

            SearchResponse sentResponse = searchSentMessages(LIMIT, SortOrder.ASC,
                    createFilterQuery(
                            filter(TOPIC_NAME, topicName),
                            filter(SUBSCRIPTION, subscriptionName),
                            filter(MESSAGE_ID, messageId)));

            return Stream.concat(
                    Arrays.stream(publishedResponse.getHits().hits()).map(this::toPublishedMessageTrace),
                    Arrays.stream(sentResponse.getHits().hits()).map(this::toSentMessageTrace))
                    .collect(toList());
        } catch (InterruptedException | ExecutionException ex) {
            throw new ElasticsearchRepositoryException(ex);
        }
    }

    private FilteredQueryBuilder createFilterQuery(FilterBuilder... filters) {
        return filteredQuery(matchAllQuery(), FilterBuilders.andFilter(filters));
    }

    private FilterBuilder filter(String fieldName, String value) {
        return FilterBuilders.termFilter(fieldName, value);
    }

    private SearchResponse searchSentMessages(int limit, SortOrder sort, QueryBuilder query) throws InterruptedException, ExecutionException {
        return elasticClient.prepareSearch(SchemaManager.SENT_ALIAS_NAME)
                .addFields(MESSAGE_ID, TIMESTAMP, SUBSCRIPTION, TOPIC_NAME, STATUS, REASON, PARTITION, OFFSET, CLUSTER)
                .setTrackScores(true)
                .setQuery(query)
                .addSort(TIMESTAMP, sort)
                .setSize(limit)
                .execute()
                .get();
    }

    private SearchResponse searchPublishedMessages(int limit, QueryBuilder query) throws InterruptedException, ExecutionException {
        return elasticClient.prepareSearch(SchemaManager.PUBLISHED_ALIAS_NAME)
                .addFields(MESSAGE_ID, TIMESTAMP, TOPIC_NAME, STATUS, REASON, CLUSTER)
                .setTrackScores(true)
                .setQuery(query)
                .addSort(TIMESTAMP, SortOrder.ASC)
                .setSize(limit)
                .execute()
                .get();
    }

    private PublishedMessageTrace toPublishedMessageTrace(SearchHit h) {
        return new PublishedMessageTrace(h.field(MESSAGE_ID).getValue(),
                h.field(TIMESTAMP).<Number>getValue().longValue(),
                h.field(TOPIC_NAME).getValue(),
                PublishedMessageTraceStatus.valueOf(h.field(STATUS).getValue()),
                h.getFields().containsKey(REASON) ? h.field(REASON).getValue() : null,
                null,
                h.field(CLUSTER).getValue());
    }

    private SentMessageTrace toSentMessageTrace(SearchHit h) {
        return new SentMessageTrace(h.field(MESSAGE_ID).getValue(),
                h.field(BATCH_ID).getValue(),
                h.field(TIMESTAMP).<Number>getValue().longValue(),
                h.field(SUBSCRIPTION).getValue(),
                h.field(TOPIC_NAME).getValue(),
                SentMessageTraceStatus.valueOf(h.field(STATUS).getValue()),
                h.getFields().containsKey(REASON) ? h.field(REASON).getValue() : null,
                null,
                h.field(PARTITION).getValue(),
                h.field(OFFSET).<Number>getValue().longValue(),
                h.field(CLUSTER).getValue());
    }
}
