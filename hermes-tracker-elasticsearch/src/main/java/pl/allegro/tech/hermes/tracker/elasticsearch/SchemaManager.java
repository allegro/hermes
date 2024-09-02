package pl.allegro.tech.hermes.tracker.elasticsearch;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static pl.allegro.tech.hermes.tracker.elasticsearch.LogSchemaAware.BATCH_ID;
import static pl.allegro.tech.hermes.tracker.elasticsearch.LogSchemaAware.CLUSTER;
import static pl.allegro.tech.hermes.tracker.elasticsearch.LogSchemaAware.EXTRA_REQUEST_HEADERS;
import static pl.allegro.tech.hermes.tracker.elasticsearch.LogSchemaAware.MESSAGE_ID;
import static pl.allegro.tech.hermes.tracker.elasticsearch.LogSchemaAware.OFFSET;
import static pl.allegro.tech.hermes.tracker.elasticsearch.LogSchemaAware.PARTITION;
import static pl.allegro.tech.hermes.tracker.elasticsearch.LogSchemaAware.PUBLISH_TIMESTAMP;
import static pl.allegro.tech.hermes.tracker.elasticsearch.LogSchemaAware.REASON;
import static pl.allegro.tech.hermes.tracker.elasticsearch.LogSchemaAware.REMOTE_HOSTNAME;
import static pl.allegro.tech.hermes.tracker.elasticsearch.LogSchemaAware.SOURCE_HOSTNAME;
import static pl.allegro.tech.hermes.tracker.elasticsearch.LogSchemaAware.STATUS;
import static pl.allegro.tech.hermes.tracker.elasticsearch.LogSchemaAware.STORAGE_DATACENTER;
import static pl.allegro.tech.hermes.tracker.elasticsearch.LogSchemaAware.SUBSCRIPTION;
import static pl.allegro.tech.hermes.tracker.elasticsearch.LogSchemaAware.TIMESTAMP;
import static pl.allegro.tech.hermes.tracker.elasticsearch.LogSchemaAware.TIMESTAMP_SECONDS;
import static pl.allegro.tech.hermes.tracker.elasticsearch.LogSchemaAware.TOPIC_NAME;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Function;
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
  private final boolean dynamicMappingEnabled;

  public static SchemaManager schemaManagerWithDailyIndexes(Client elasticClient) {
    return new SchemaManager(
        elasticClient, new FrontendDailyIndexFactory(), new ConsumersDailyIndexFactory());
  }

  public SchemaManager(
      Client client,
      FrontendIndexFactory frontendIndexFactory,
      ConsumersIndexFactory consumersIndexFactory) {
    this(client, frontendIndexFactory, consumersIndexFactory, true);
  }

  public SchemaManager(
      Client client,
      FrontendIndexFactory frontendIndexFactory,
      ConsumersIndexFactory consumersIndexFactory,
      boolean dynamicMappingEnabled) {
    this.client = client;
    this.frontendIndexFactory = frontendIndexFactory;
    this.consumersIndexFactory = consumersIndexFactory;
    this.dynamicMappingEnabled = dynamicMappingEnabled;
  }

  public void ensureSchema() {
    createTemplate(
        PUBLISHED_TEMPLATE_NAME,
        PUBLISHED_TYPE,
        PUBLISHED_INDICES_REG_EXP,
        PUBLISHED_ALIAS_NAME,
        preparePublishedMapping());
    createTemplate(
        SENT_TEMPLATE_NAME, SENT_TYPE, SENT_INDICES_REG_EXP, SENT_ALIAS_NAME, prepareSentMapping());

    createIndexIfNeeded(frontendIndexFactory);
    createIndexIfNeeded(consumersIndexFactory);

    createAlias(frontendIndexFactory, PUBLISHED_ALIAS_NAME);
    createAlias(consumersIndexFactory, SENT_ALIAS_NAME);
  }

  private void createIndexIfNeeded(IndexFactory indexFactory) {
    IndicesExistsResponse response =
        client
            .admin()
            .indices()
            .exists(new IndicesExistsRequest(indexFactory.createIndex()))
            .actionGet();

    if (response.isExists()) {
      return;
    }

    client.admin().indices().prepareCreate(indexFactory.createIndex()).execute().actionGet();
  }

  private void createAlias(IndexFactory indexFactory, String alias) {
    client
        .admin()
        .indices()
        .prepareAliases()
        .addAlias(indexFactory.createIndex(), alias)
        .execute()
        .actionGet();
  }

  private void createTemplate(
      String templateName,
      String indexType,
      String indicesRegExp,
      String aliasName,
      XContentBuilder templateMapping) {

    PutIndexTemplateRequest publishedTemplateRequest =
        new PutIndexTemplateRequest(templateName)
            .patterns(Arrays.asList(indicesRegExp))
            .mapping(indexType, templateMapping)
            .alias(new Alias(aliasName));

    client.admin().indices().putTemplate(publishedTemplateRequest).actionGet();
  }

  private XContentBuilder preparePublishedMapping() {
    return prepareMapping(PUBLISHED_TYPE, Function.identity());
  }

  private XContentBuilder prepareSentMapping() {
    return prepareMapping(
        SENT_TYPE,
        contentBuilder -> {
          try {
            return contentBuilder
                .startObject(SUBSCRIPTION)
                .field("type", "keyword")
                .field("norms", false)
                .endObject()
                .startObject(PUBLISH_TIMESTAMP)
                .field("type", "date")
                .field("index", false)
                .endObject()
                .startObject(BATCH_ID)
                .field("type", "keyword")
                .field("norms", false)
                .endObject()
                .startObject(OFFSET)
                .field("type", "long")
                .endObject()
                .startObject(PARTITION)
                .field("type", "integer")
                .endObject();
          } catch (IOException e) {
            throw new ElasticsearchRepositoryException(e);
          }
        });
  }

  private XContentBuilder prepareMapping(
      String indexType, Function<XContentBuilder, XContentBuilder> additionalMapping) {
    try {
      XContentBuilder jsonBuilder =
          jsonBuilder()
              .startObject()
              .startObject(indexType)
              .field("dynamic", dynamicMappingEnabled)
              .startObject("properties")
              .startObject(MESSAGE_ID)
              .field("type", "keyword")
              .field("norms", false)
              .endObject()
              .startObject(TIMESTAMP)
              .field("type", "date")
              .field("index", false)
              .endObject()
              .startObject(TIMESTAMP_SECONDS)
              .field("type", "date")
              .field("format", "epoch_second")
              .endObject()
              .startObject(TOPIC_NAME)
              .field("type", "keyword")
              .field("norms", false)
              .endObject()
              .startObject(STATUS)
              .field("type", "keyword")
              .field("norms", false)
              .endObject()
              .startObject(CLUSTER)
              .field("type", "keyword")
              .field("norms", false)
              .endObject()
              .startObject(SOURCE_HOSTNAME)
              .field("type", "keyword")
              .field("norms", false)
              .endObject()
              .startObject(REMOTE_HOSTNAME)
              .field("type", "keyword")
              .field("norms", false)
              .endObject()
              .startObject(REASON)
              .field("type", "text")
              .field("norms", false)
              .endObject()
              .startObject(STORAGE_DATACENTER)
              .field("type", "text")
              .field("norms", false)
              .endObject()
              .startObject(EXTRA_REQUEST_HEADERS)
              .field("type", "text")
              .field("norms", false)
              .endObject();

      return additionalMapping.apply(jsonBuilder).endObject().endObject().endObject();
    } catch (IOException ex) {
      throw new ElasticsearchRepositoryException(ex);
    }
  }
}
