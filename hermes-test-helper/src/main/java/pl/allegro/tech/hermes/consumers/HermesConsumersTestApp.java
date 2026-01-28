package pl.allegro.tech.hermes.consumers;

import static pl.allegro.tech.hermes.consumers.ConsumersConfigurationProperties.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
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
  private final Map<String, Object> extraArgs = new HashMap<>();
  private Supplier<String> googlePubSubEndpoint = getDefaultPubSubEndpoint();

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
    Map<String, Object> args = new HashMap<>();
    args.put(SPRING_PROFILES_ACTIVE, "integration");
    args.put(CONSUMER_HEALTH_CHECK_PORT, 0);
    args.put(CONSUMER_KAFKA_NAMESPACE, "itTest");
    args.put(CONSUMER_KAFKA_CLUSTER_BROKER_LIST, kafka.getBootstrapServersForExternalClients());
    args.put(CONSUMER_KAFKA_CLUSTER_NAME, "primary-dc");
    args.put(CONSUMER_ZOOKEEPER_CONNECTION_STRING, hermesZookeeper.getConnectionString());
    args.put(CONSUMER_SCHEMA_REPOSITORY_SERVER_URL, schemaRegistry.getUrl());
    args.put(CONSUMER_BACKGROUND_SUPERVISOR_INTERVAL, Duration.ofMillis(100));
    args.put(CONSUMER_WORKLOAD_REBALANCE_INTERVAL, Duration.ofSeconds(1));
    args.put(CONSUMER_COMMIT_OFFSET_PERIOD, Duration.ofSeconds(1));
    args.put(CONSUMER_METRICS_MICROMETER_REPORT_PERIOD, Duration.ofSeconds(5));
    args.put(CONSUMER_SCHEMA_CACHE_ENABLED, true);
    args.put(CONSUMER_GOOGLE_PUBSUB_TRANSPORT_CHANNEL_PROVIDER_ADDRESS, googlePubSubEndpoint.get());
    args.putAll(extraArgs);
    return args.entrySet().stream()
        .map(entry -> "--" + entry.getKey() + "=" + entry.getValue())
        .toList();
  }

  public void withGooglePubSubEndpoint(Supplier<String> googlePubSub) {
    this.googlePubSubEndpoint = googlePubSub;
  }

  private static Supplier<String> getDefaultPubSubEndpoint() {
    return () -> "integration";
  }

  @Override
  public void restoreDefaultSettings() {
    extraArgs.clear();
    googlePubSubEndpoint = getDefaultPubSubEndpoint();
  }

  public HermesConsumersTestApp withProperty(String name, Object value) {
    this.extraArgs.put(name, value);
    return this;
  }
}
