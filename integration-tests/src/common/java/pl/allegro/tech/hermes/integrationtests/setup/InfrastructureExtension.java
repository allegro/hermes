package pl.allegro.tech.hermes.integrationtests.setup;

import java.util.stream.Stream;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.lifecycle.Startable;
import pl.allegro.tech.hermes.test.helper.containers.ConfluentSchemaRegistryContainer;
import pl.allegro.tech.hermes.test.helper.containers.KafkaContainerCluster;
import pl.allegro.tech.hermes.test.helper.containers.ZookeeperContainer;

public class InfrastructureExtension
    implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {
  private static final KafkaContainerCluster kafka = new KafkaContainerCluster(1);
  private static final ZookeeperContainer hermesZookeeper =
      new ZookeeperContainer("HermesZookeeper");
  private static final ConfluentSchemaRegistryContainer schemaRegistry =
      new ConfluentSchemaRegistryContainer().withKafkaCluster(kafka);

  private static boolean started = false;

  public KafkaContainerCluster kafka() {
    return kafka;
  }

  public ZookeeperContainer hermesZookeeper() {
    return hermesZookeeper;
  }

  public ConfluentSchemaRegistryContainer schemaRegistry() {
    return schemaRegistry;
  }

  @Override
  public void beforeAll(ExtensionContext context) {
    if (!started) {
      Stream.of(hermesZookeeper, kafka).parallel().forEach(Startable::start);
      schemaRegistry.start();
      started = true;
    }
  }

  @Override
  public void close() {
    Stream.of(hermesZookeeper, kafka, schemaRegistry).parallel().forEach(Startable::stop);
    started = false;
  }
}
