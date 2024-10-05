package pl.allegro.tech.hermes.tracker.elasticsearch;

import static pl.allegro.tech.embeddedelasticsearch.PopularProperties.CLUSTER_NAME;
import static pl.allegro.tech.embeddedelasticsearch.PopularProperties.HTTP_PORT;
import static pl.allegro.tech.embeddedelasticsearch.PopularProperties.TRANSPORT_TCP_PORT;

import java.net.InetAddress;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.junit.rules.ExternalResource;
import pl.allegro.tech.embeddedelasticsearch.EmbeddedElastic;
import pl.allegro.tech.hermes.test.helper.util.Ports;

public class ElasticsearchResource extends ExternalResource implements LogSchemaAware {

  private static final String ELASTIC_VERSION = "6.1.4";
  private static final String CLUSTER_NAME_VALUE = "myTestCluster";

  private final EmbeddedElastic embeddedElastic;
  private Client client;

  public ElasticsearchResource() {
    int port = Ports.nextAvailable();
    int httpPort = Ports.nextAvailable();

    try {
      embeddedElastic =
          EmbeddedElastic.builder()
              .withElasticVersion(ELASTIC_VERSION)
              .withSetting(TRANSPORT_TCP_PORT, port)
              .withSetting(HTTP_PORT, httpPort)
              .withSetting(CLUSTER_NAME, CLUSTER_NAME_VALUE)
              // embedded elastic search runs with "UseConcMarkSweepGC" which is invalid in Java 17
              .withEsJavaOpts("-Xms128m -Xmx512m -XX:+IgnoreUnrecognizedVMOptions")
              .withStartTimeout(1, TimeUnit.MINUTES)
              .withCleanInstallationDirectoryOnStop(true)
              .withInstallationDirectory(
                  Files.createTempDirectory("elasticsearch-installation-" + port).toFile())
              .build();

    } catch (Exception e) {
      throw new RuntimeException("Unchecked exception", e);
    }
  }

  @Override
  public void before() throws Throwable {
    embeddedElastic.start();

    client =
        new PreBuiltTransportClient(
                Settings.builder().put(CLUSTER_NAME, CLUSTER_NAME_VALUE).build())
            .addTransportAddress(
                new TransportAddress(
                    InetAddress.getByName("localhost"), embeddedElastic.getTransportTcpPort()));
  }

  @Override
  public void after() {
    embeddedElastic.stop();
    client.close();
  }

  public Client client() {
    return client;
  }

  public AdminClient adminClient() {
    return client.admin();
  }

  public ImmutableOpenMap<String, IndexMetaData> getIndices() {
    return client
        .admin()
        .cluster()
        .prepareState()
        .execute()
        .actionGet()
        .getState()
        .getMetaData()
        .getIndices();
  }

  public void cleanStructures() {
    embeddedElastic.deleteIndices();
    embeddedElastic.deleteTemplates();
  }
}
