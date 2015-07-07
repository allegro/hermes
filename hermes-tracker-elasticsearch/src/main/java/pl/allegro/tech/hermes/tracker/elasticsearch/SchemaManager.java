package pl.allegro.tech.hermes.tracker.elasticsearch;

import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequest;
import org.elasticsearch.client.Client;
import pl.allegro.tech.hermes.tracker.elasticsearch.consumers.ConsumersDailyIndexFactory;
import pl.allegro.tech.hermes.tracker.elasticsearch.consumers.ConsumersIndexFactory;
import pl.allegro.tech.hermes.tracker.elasticsearch.frontend.FrontendDailyIndexFactory;
import pl.allegro.tech.hermes.tracker.elasticsearch.frontend.FrontendIndexFactory;

import java.util.concurrent.ExecutionException;

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
        createIndexIfNeeded(frontendIndexFactory, PUBLISHED_INDICES_REG_EXP);
        createIndexIfNeeded(consumersIndexFactory, SENT_INDICES_REG_EXP);

        createAlias(frontendIndexFactory, PUBLISHED_ALIAS_NAME);
        createAlias(consumersIndexFactory, SENT_ALIAS_NAME);

        createTemplate(PUBLISHED_TEMPLATE_NAME, PUBLISHED_INDICES_REG_EXP, PUBLISHED_ALIAS_NAME);
        createTemplate(SENT_TEMPLATE_NAME, SENT_INDICES_REG_EXP, SENT_ALIAS_NAME);
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
        try {
            client.admin().indices().prepareAliases().addAlias(indexFactory.createIndex(), alias).execute().get();
        } catch (ExecutionException | InterruptedException ex) {
            throw new ElasticsearchRepositoryException(ex);
        }
    }

    private void createTemplate(String templateName, String indicesRegExp, String aliasName) {
        PutIndexTemplateRequest publishedTemplateRequest = new PutIndexTemplateRequest(templateName)
                .template(indicesRegExp)
                .alias(new Alias(aliasName));

        try {
            client.admin().indices().putTemplate(publishedTemplateRequest).get();
        } catch (ExecutionException | InterruptedException ex) {
            throw new ElasticsearchRepositoryException(ex);
        }
    }
}
