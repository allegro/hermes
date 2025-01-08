package pl.allegro.tech.hermes.tracker.elasticsearch;

import java.net.InetAddress;
import java.time.Duration;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.IndexMetadata;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.junit.rules.ExternalResource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

public class ElasticsearchResource extends ExternalResource implements LogSchemaAware {

  private static final String ELASTIC_VERSION =
      "docker.elastic.co/elasticsearch/elasticsearch:7.10.2";
  private static final String CLUSTER_NAME_VALUE = "myTestCluster";

  private final ElasticsearchContainer elasticsearchContainer;
  private Client client;

  public ElasticsearchResource() {
    try {
      elasticsearchContainer =
          new ElasticsearchContainer(ELASTIC_VERSION)
              .withEnv("cluster.name", CLUSTER_NAME_VALUE)
              .withEnv("discovery.type", "single-node")
              .withEnv("ES_JAVA_OPTS", "-Xms128m -Xmx512m -XX:+IgnoreUnrecognizedVMOptions")
              .withStartupTimeout(Duration.ofMinutes(1));
    } catch (Exception e) {
      throw new RuntimeException("Unchecked exception", e);
    }
  }

  @Override
  public void before() throws Throwable {
    elasticsearchContainer.start();

    client =
        new PreBuiltTransportClient(
                Settings.builder().put("cluster.name", CLUSTER_NAME_VALUE).build())
            .addTransportAddress(
                new TransportAddress(
                    InetAddress.getByName(elasticsearchContainer.getHost()),
                    elasticsearchContainer.getMappedPort(9300)));
  }

  @Override
  public void after() {
    elasticsearchContainer.stop();
    client.close();
  }

  public Client client() {
    return client;
  }

  public AdminClient adminClient() {
    return client.admin();
  }

  public ImmutableOpenMap<String, IndexMetadata> getIndices() {
    return client
        .admin()
        .cluster()
        .prepareState()
        .execute()
        .actionGet()
        .getState()
        .getMetadata()
        .getIndices();
  }

  public void cleanStructures() {
    client.admin().indices().prepareDelete("*").get();
    client.admin().indices().prepareDeleteTemplate("*").get();
  }
}
