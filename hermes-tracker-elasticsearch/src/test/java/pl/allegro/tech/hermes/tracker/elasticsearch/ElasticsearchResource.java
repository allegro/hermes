package pl.allegro.tech.hermes.tracker.elasticsearch;

import com.google.common.collect.ImmutableMap;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.io.FileSystemUtils;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.junit.rules.ExternalResource;

import java.io.File;
import java.nio.file.Files;

public class ElasticsearchResource extends ExternalResource implements LogSchemaAware {

    private final TypedIndex[] indices;
    private Node elastic;
    private Client client;
    private File dataDir;

    public ElasticsearchResource(TypedIndex... indices) {
        this.indices = indices;
    }

    @Override
    protected void before() throws Throwable {
        dataDir = Files.createTempDirectory("elasticsearch_data_").toFile();
        Settings settings = ImmutableSettings.settingsBuilder()
                .put("path.data", dataDir)
                .put("cluster.name", "hermes").build();
        elastic = NodeBuilder.nodeBuilder().local(true).settings(settings).build();
        elastic.start();
        client = elastic.client();

        createIndices();
    }

    private void createIndices() {
        for (TypedIndex index : indices) {
            client.admin().indices()
                    .prepareCreate(index.getIndex())
                    .addMapping(index.getType(), ImmutableMap.of("properties", ImmutableMap.of(TIMESTAMP, ImmutableMap.of("type", "long"))))
                    .execute().actionGet();
            client.admin().cluster().prepareHealth(index.getIndex()).setWaitForActiveShards(1).execute().actionGet();
        }
    }

    @Override
    protected void after() {
        elastic.stop();
        FileSystemUtils.deleteRecursively(dataDir);
    }

    public Client client() {
        return client;
    }

}
