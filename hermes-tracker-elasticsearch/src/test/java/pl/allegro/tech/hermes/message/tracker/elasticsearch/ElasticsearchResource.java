package pl.allegro.tech.hermes.message.tracker.elasticsearch;

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

    private final String[] indices;
    private Node elastic;
    private Client client;
    private File dataDir;

    public ElasticsearchResource(String... indices) {
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
        for (String index : indices) {
            client.admin().indices()
                    .prepareCreate(index)
                    .execute().actionGet();
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
