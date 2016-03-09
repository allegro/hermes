package pl.allegro.tech.hermes.tracker.elasticsearch;

import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import pl.allegro.tech.hermes.tracker.elasticsearch.consumers.ConsumersDailyIndexFactory;
import pl.allegro.tech.hermes.tracker.elasticsearch.consumers.ConsumersIndexFactory;
import pl.allegro.tech.hermes.tracker.elasticsearch.frontend.FrontendDailyIndexFactory;
import pl.allegro.tech.hermes.tracker.elasticsearch.frontend.FrontendIndexFactory;

import java.io.IOException;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static pl.allegro.tech.hermes.tracker.elasticsearch.LogSchemaAware.BATCH_ID;
import static pl.allegro.tech.hermes.tracker.elasticsearch.LogSchemaAware.CLUSTER;
import static pl.allegro.tech.hermes.tracker.elasticsearch.LogSchemaAware.MESSAGE_ID;
import static pl.allegro.tech.hermes.tracker.elasticsearch.LogSchemaAware.REASON;
import static pl.allegro.tech.hermes.tracker.elasticsearch.LogSchemaAware.STATUS;
import static pl.allegro.tech.hermes.tracker.elasticsearch.LogSchemaAware.SUBSCRIPTION;
import static pl.allegro.tech.hermes.tracker.elasticsearch.LogSchemaAware.TOPIC_NAME;

public class SchemaManager {

    public static final String PUBLISHED_INDEX = "published_messages";
    public static final String PUBLISHED_TYPE = "published_message";
    public static final String PUBLISHED_ALIAS_NAME = "alias_published_messages";
    public static final String PUBLISHED_TEMPLATE_NAME = "template_published_messages";
    public static final String PUBLISHED_INDICES_REG_EXP = "published_messages_*";

    public static final String SENT_INDEX = "sent_messages";
    public static final String SENT_TYPE = "sent_message";
    public static final String SENT_ALIAS_NAME = "alias_sent_messages";
    public static final String SENT_TEMPLATE_NAME = "template_sent_messages";
    public static final String SENT_INDICES_REG_EXP = "sent_messages_*";

    private final Client client;
    private final FrontendIndexFactory frontendIndexFactory;
    private final ConsumersIndexFactory consumersIndexFactory;

    public static SchemaManager schemaManagerWithDailyIndexes(Client elasticClient) {
        return new SchemaManager(elasticClient, new FrontendDailyIndexFactory(), new ConsumersDailyIndexFactory());
    }

    public SchemaManager(Client client, FrontendIndexFactory frontendIndexFactory, ConsumersIndexFactory consumersIndexFactory) {
        this.client = client;
        this.frontendIndexFactory = frontendIndexFactory;
        this.consumersIndexFactory = consumersIndexFactory;
    }

    public void ensureSchema() {
        createTemplate(PUBLISHED_TEMPLATE_NAME, PUBLISHED_TYPE, PUBLISHED_INDICES_REG_EXP, PUBLISHED_ALIAS_NAME);
        createTemplate(SENT_TEMPLATE_NAME, SENT_TYPE, SENT_INDICES_REG_EXP, SENT_ALIAS_NAME);

        createIndexIfNeeded(frontendIndexFactory, PUBLISHED_INDICES_REG_EXP);
        createIndexIfNeeded(consumersIndexFactory, SENT_INDICES_REG_EXP);

        createAlias(frontendIndexFactory, PUBLISHED_ALIAS_NAME);
        createAlias(consumersIndexFactory, SENT_ALIAS_NAME);
    }

    private void createIndexIfNeeded(IndexFactory indexFactory, String indexRegExp) {
        IndicesExistsResponse response =
                client.admin().indices().exists(new IndicesExistsRequest(indexRegExp)).actionGet();

        if (response.isExists()) {
            return;
        }

        client.admin().indices().prepareCreate(indexFactory.createIndex()).execute().actionGet();
    }

    private void createAlias(IndexFactory indexFactory, String alias) {
        client.admin().indices().prepareAliases()
                .addAlias(indexFactory.createIndex(), alias)
                .execute().actionGet();
    }

    private void createTemplate(String templateName, String indexType, String indicesRegExp, String aliasName) {

        PutIndexTemplateRequest publishedTemplateRequest = new PutIndexTemplateRequest(templateName)
                .template(indicesRegExp)
                .mapping(indexType, prepareMapping(indexType))
                .alias(new Alias(aliasName));

        client.admin().indices().putTemplate(publishedTemplateRequest).actionGet();
    }

    private XContentBuilder prepareMapping(String indexType) {
        try {
            return jsonBuilder()
                    .startObject().startObject(indexType)
                    .startObject("_all").field("enabled", false).endObject()
                    .startObject("properties")
                    .startObject(MESSAGE_ID).field("type", "string").field("index", "not_analyzed").endObject()
                    .startObject(BATCH_ID).field("type", "string").field("index", "not_analyzed").endObject()
                    .startObject(STATUS).field("type", "string").field("index", "not_analyzed").endObject()
                    .startObject(TOPIC_NAME).field("type", "string").field("index", "not_analyzed").endObject()
                    .startObject(SUBSCRIPTION).field("type", "string").field("index", "not_analyzed").endObject()
                    .startObject(CLUSTER).field("type", "string").field("index", "not_analyzed").endObject()
                    .startObject(REASON).field("type", "string").field("index", "not_analyzed").endObject()
                    .endObject().endObject();
        } catch (IOException ex) {
            throw new ElasticsearchRepositoryException(ex);
        }
    }
}
