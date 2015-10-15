package pl.allegro.tech.hermes.tracker.elasticsearch;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.io.FileSystemUtils;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import java.io.File;
import java.nio.file.Files;

public class ElasticsearchResource implements LogSchemaAware {

    private final IndexFactory[] indices;
    private Node elastic;
    private Client client;
    private File dataDir;

    public ElasticsearchResource(IndexFactory... indices) {
        this.indices = indices;
    }

    public void before() throws Throwable {
        dataDir = Files.createTempDirectory("elasticsearch_data_").toFile();
        Settings settings = ImmutableSettings.settingsBuilder()
                .put("path.data", dataDir)
                .put("cluster.name", "hermes").build();
        elastic = NodeBuilder.nodeBuilder().local(true).settings(settings).build();
        elastic.start();
        client = elastic.client();
    }

    public void after() {
        elastic.stop();
        FileSystemUtils.deleteRecursively(dataDir);
    }

    public Client client() {
        return client;
    }

}
