package pl.allegro.tech.hermes.tracker.elasticsearch.management;

import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
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
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static pl.allegro.tech.hermes.tracker.elasticsearch.SchemaManager.schemaManagerWithDailIndexes;

public class ElasticsearchLogRepository implements LogRepository, LogSchemaAware {

    private static final int LIMIT = 1000;

    private final Client elasticClient;
    private final float minScore;

    public ElasticsearchLogRepository(Client elasticClient, float minScore) {
        this(elasticClient, minScore, schemaManagerWithDailIndexes(elasticClient));
    }

    public ElasticsearchLogRepository(Client elasticClient, float minScore, SchemaManager schemaManager) {
        this.elasticClient = elasticClient;
        this.minScore = minScore;

        schemaManager.ensureSchema();
    }

    @Override
    public List<SentMessageTrace> getLastUndeliveredMessages(String topicName, String subscriptionName, int limit) {

        try {
            SearchResponse response = searchSentMessages(limit, createQuery(topicName, subscriptionName)
                    .must(matchQuery(STATUS, SentMessageTraceStatus.DISCARDED)));

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
            SearchResponse publishedResponse = searchPublishedMessages(LIMIT, createQuery(topicName).must(matchQuery(MESSAGE_ID, messageId)));
            SearchResponse sentResponse = searchSentMessages(LIMIT, createQuery(topicName, subscriptionName).must(matchQuery(MESSAGE_ID, messageId)));

            return Stream.concat(
                    Arrays.stream(publishedResponse.getHits().hits()).map(this::toPublishedMessageTrace),
                    Arrays.stream(sentResponse.getHits().hits()).map(this::toSentMessageTrace))
                    .collect(toList());
        } catch (InterruptedException | ExecutionException ex) {
            throw new ElasticsearchRepositoryException(ex);
        }
    }

    private void createTemplate(String template, String index, String alias) {
        PutIndexTemplateRequest publishedTemplateRequest = new PutIndexTemplateRequest(template)
                .template(index)
                .alias(new Alias(alias));

        try {
            elasticClient.admin().indices().putTemplate(publishedTemplateRequest).get();
        } catch (ExecutionException | InterruptedException ex) {
            throw new ElasticsearchRepositoryException(ex);
        }
    }

    private BoolQueryBuilder createQuery(String topicName) {
        return boolQuery()
                .must(matchQuery(TOPIC_NAME, topicName));
    }

    private BoolQueryBuilder createQuery(String topicName, String subscriptionName) {
        return createQuery(topicName)
                .must(matchQuery(SUBSCRIPTION, subscriptionName));
    }

    private SearchResponse searchSentMessages(int limit, QueryBuilder query) throws InterruptedException, ExecutionException {
        return elasticClient.prepareSearch(SchemaManager.SENT_ALIAS_NAME)
                .addFields(MESSAGE_ID, TIMESTAMP, SUBSCRIPTION, TOPIC_NAME, STATUS, REASON, PARTITION, OFFSET, CLUSTER)
                .setTrackScores(true)
                .setQuery(query)
                .addSort(TIMESTAMP, SortOrder.ASC)
                .setMinScore(minScore)
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
                .setMinScore(minScore)
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
