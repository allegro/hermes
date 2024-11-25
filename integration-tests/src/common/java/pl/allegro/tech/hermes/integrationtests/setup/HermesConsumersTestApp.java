package pl.allegro.tech.hermes.integrationtests.setup;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import pl.allegro.tech.hermes.consumers.HermesConsumers;
import pl.allegro.tech.hermes.consumers.server.ConsumerHttpServer;
import pl.allegro.tech.hermes.test.helper.containers.ConfluentSchemaRegistryContainer;
import pl.allegro.tech.hermes.test.helper.containers.KafkaContainerCluster;
import pl.allegro.tech.hermes.test.helper.containers.ZookeeperContainer;
import pl.allegro.tech.hermes.test.helper.environment.HermesTestApp;

public class HermesConsumersTestApp implements HermesTestApp {

  private final ZookeeperContainer hermesZookeeper;
  private final KafkaContainerCluster kafka;
  private final ConfluentSchemaRegistryContainer schemaRegistry;

  private int port = -1;
  private SpringApplicationBuilder app = null;
  private List<String> currentArgs = List.of();
  private GooglePubSubExtension googlePubSub = null;

  public HermesConsumersTestApp(
      ZookeeperContainer hermesZookeeper,
      KafkaContainerCluster kafka,
      ConfluentSchemaRegistryContainer schemaRegistry) {
    this.hermesZookeeper = hermesZookeeper;
    this.kafka = kafka;
    this.schemaRegistry = schemaRegistry;
  }

  @Override
  public HermesTestApp start() {
    app = new SpringApplicationBuilder(HermesConsumers.class).web(WebApplicationType.NONE);
    currentArgs = createArgs();
    app.run(currentArgs.toArray(new String[0]));
    port = app.context().getBean(ConsumerHttpServer.class).getPort();
    return this;
  }

  @Override
  public void stop() {
    if (app != null) {
      app.context().close();
      app = null;
    }
  }

  @Override
  public int getPort() {
    if (port == -1) {
      throw new IllegalStateException("hermes-consumers port hasn't been initialized");
    }
    return port;
  }

  @Override
  public boolean shouldBeRestarted() {
    List<String> args = createArgs();
    return !args.equals(currentArgs);
  }

  private List<String> createArgs() {
    return List.of(
        "--spring.profiles.active=integration",
        "--consumer.healthCheckPort=0",
        "--consumer.kafka.namespace=itTest",
        "--consumer.kafka.clusters.[0].brokerList=" + kafka.getBootstrapServersForExternalClients(),
        "--consumer.kafka.clusters.[0].clusterName=" + "primary-dc",
        "--consumer.zookeeper.clusters.[0].connectionString="
            + hermesZookeeper.getConnectionString(),
        "--consumer.schema.repository.serverUrl=" + schemaRegistry.getUrl(),
        "--consumer.backgroundSupervisor.interval=" + Duration.ofMillis(100),
        "--consumer.workload.rebalanceInterval=" + Duration.ofSeconds(1),
        "--consumer.commit.offset.period=" + Duration.ofSeconds(1),
        "--consumer.metrics.micrometer.reportPeriod=" + Duration.ofSeconds(5),
        "--consumer.schema.cache.enabled=true",
        "--consumer.google.pubsub.sender.transportChannelProviderAddress="
            + getGooglePubSubEndpoint());
  }

  private String getGooglePubSubEndpoint() {
    if (googlePubSub == null) {
      return "integration";
    }
    return googlePubSub.getEmulatorEndpoint();
  }

  void withGooglePubSubEndpoint(GooglePubSubExtension googlePubSub) {
    this.googlePubSub = googlePubSub;
  }

  @Override
  public void restoreDefaultSettings() {
    googlePubSub = null;
  }
}
