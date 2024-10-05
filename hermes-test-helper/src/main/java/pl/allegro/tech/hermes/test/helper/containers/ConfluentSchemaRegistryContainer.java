package pl.allegro.tech.hermes.test.helper.containers;

import static java.lang.String.format;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class ConfluentSchemaRegistryContainer
    extends GenericContainer<ConfluentSchemaRegistryContainer> {
  private static final DockerImageName DEFAULT_SCHEMA_REGISTRY_IMAGE_NAME =
      DockerImageName.parse("confluentinc/cp-schema-registry")
          .withTag(ImageTags.confluentImagesTag());
  private static final int SCHEMA_REGISTRY_PORT = 8081;

  public ConfluentSchemaRegistryContainer() {
    super(DEFAULT_SCHEMA_REGISTRY_IMAGE_NAME);
    addEnv("SCHEMA_REGISTRY_HOST_NAME", "localhost");
    withExposedPorts(SCHEMA_REGISTRY_PORT);
    withNetworkAliases("schema-registry");
    waitingFor(Wait.forHttp("/subjects"));
  }

  public ConfluentSchemaRegistryContainer withKafkaCluster(KafkaContainerCluster cluster) {
    withNetwork(cluster.getNetwork());
    withEnv(
        "SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS",
        "PLAINTEXT://" + cluster.getBootstrapServersForInternalClients());
    return self();
  }

  public String getUrl() {
    return format("http://%s:%d", getHost(), getMappedPort(SCHEMA_REGISTRY_PORT));
  }
}
