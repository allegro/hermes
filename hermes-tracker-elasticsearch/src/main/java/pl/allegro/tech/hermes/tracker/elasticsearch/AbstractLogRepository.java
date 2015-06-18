package pl.allegro.tech.hermes.tracker.elasticsearch;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.util.concurrent.Callable;

public abstract class AbstractLogRepository implements LogSchemaAware {

    protected final String clusterName;
    private final Client elasticClient;
    private final String indexName;
    private final String typeName;

    public AbstractLogRepository(Client elasticClient, String clusterName, String indexName, String typeName) {
        this.elasticClient = elasticClient;
        this.clusterName = clusterName;
        this.indexName = indexName;
        this.typeName = typeName;
    }

    protected void indexDocument(Callable<XContentBuilder> document) {
        try {
            elasticClient.prepareIndex(indexName, typeName)
                    .setSource(document.call())
                    .execute();
        } catch (Exception ex) {
            throw new ElasticsearchRepositoryException(ex);
        }
    }

}
