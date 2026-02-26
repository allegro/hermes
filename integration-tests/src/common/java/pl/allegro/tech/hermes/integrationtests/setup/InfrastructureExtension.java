package pl.allegro.tech.hermes.integrationtests.setup;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
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
      hermesZookeeper.start();
      kafka.start();
      schemaRegistry.start();
      started = true;
    }
  }

  @Override
  public void close() {
    hermesZookeeper.stop();
    kafka.stop();
    schemaRegistry.stop();
    started = false;
  }
}
