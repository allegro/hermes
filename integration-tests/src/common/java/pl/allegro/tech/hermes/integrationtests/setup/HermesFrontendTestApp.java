package pl.allegro.tech.hermes.integrationtests.setup;

import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.AUTH_PASSWORD;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.AUTH_USERNAME;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_FORCE_TOPIC_MAX_MESSAGE_SIZE;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_GRACEFUL_SHUTDOWN_ENABLED;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_HEADER_PROPAGATION_ALLOWED;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_HEADER_PROPAGATION_ENABLED;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_IDLE_TIMEOUT;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_MESSAGE_PREVIEW_ENABLED;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_MESSAGE_PREVIEW_LOG_PERSIST_PERIOD;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_PORT;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_READINESS_CHECK_ENABLED;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_READINESS_CHECK_INTERVAL_SECONDS;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_READINESS_CHECK_KAFKA_CHECK_ENABLED;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_THROUGHPUT_FIXED_MAX;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_THROUGHPUT_TYPE;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.KAFKA_NAMESPACE;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.KAFKA_PRODUCER_METADATA_MAX_AGE;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.METRICS_MICROMETER_REPORT_PERIOD;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.SCHEMA_CACHE_ENABLED;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.SCHEMA_REPOSITORY_SERVER_URL;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.SPRING_PROFILES_ACTIVE;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.ZOOKEEPER_CONNECTION_STRING;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import pl.allegro.tech.hermes.frontend.HermesFrontend;
import pl.allegro.tech.hermes.frontend.server.HermesServer;
import pl.allegro.tech.hermes.test.helper.containers.ConfluentSchemaRegistryContainer;
import pl.allegro.tech.hermes.test.helper.containers.KafkaContainerCluster;
import pl.allegro.tech.hermes.test.helper.containers.ZookeeperContainer;
import pl.allegro.tech.hermes.test.helper.environment.HermesTestApp;

public class HermesFrontendTestApp implements HermesTestApp {

  private final ZookeeperContainer hermesZookeeper;
  private final Map<String, KafkaContainerCluster> kafkaClusters;
  private final ConfluentSchemaRegistryContainer schemaRegistry;
  private SpringApplicationBuilder app;

  private int port = -1;
  private boolean kafkaCheckEnabled = false;
  private Duration metadataMaxAge = Duration.ofMinutes(5);
  private Duration readinessCheckInterval = Duration.ofSeconds(1);
  private final Map<String, Object> extraArgs = new HashMap<>();
  private final List<String> profiles = new ArrayList<>(List.of("integration"));
  private List<String> currentArgs = List.of();

  public HermesFrontendTestApp(
      ZookeeperContainer hermesZookeeper,
      KafkaContainerCluster kafka,
      ConfluentSchemaRegistryContainer schemaRegistry) {
    this.hermesZookeeper = hermesZookeeper;
    this.schemaRegistry = schemaRegistry;
    this.kafkaClusters = Map.of("dc", kafka);
  }

  public HermesFrontendTestApp(
      ZookeeperContainer hermesZookeeper,
      Map<String, KafkaContainerCluster> kafkaClusters,
      ConfluentSchemaRegistryContainer schemaRegistry) {
    this.hermesZookeeper = hermesZookeeper;
    this.schemaRegistry = schemaRegistry;
    this.kafkaClusters = kafkaClusters;
  }

  private String kafkaClusterProperty(int index, String name) {
    return String.format("frontend.kafka.clusters[%d].%s", index, name);
  }

  public HermesFrontendTestApp withProperty(String name, Object value) {
    this.extraArgs.put(name, value);
    return this;
  }

  public HermesFrontendTestApp withSpringProfile(String profile) {
    profiles.add(profile);
    return this;
  }

  private List<String> createArgs() {
    Map<String, Object> args = new HashMap<>();
    args.put(SPRING_PROFILES_ACTIVE, String.join(",", profiles));
    args.put(FRONTEND_PORT, 0);

    args.put(KAFKA_NAMESPACE, "itTest");

    var i = 0;
    for (var entry : kafkaClusters.entrySet()) {
      args.put(kafkaClusterProperty(i, "datacenter"), entry.getKey());
      args.put(
          kafkaClusterProperty(i, "brokerList"),
          entry.getValue().getBootstrapServersForExternalClients());
      i++;
    }

    args.put(ZOOKEEPER_CONNECTION_STRING, hermesZookeeper.getConnectionString());

    args.put(SCHEMA_CACHE_ENABLED, true);
    args.put(SCHEMA_REPOSITORY_SERVER_URL, schemaRegistry.getUrl());

    args.put(FRONTEND_READINESS_CHECK_KAFKA_CHECK_ENABLED, kafkaCheckEnabled);
    args.put(FRONTEND_READINESS_CHECK_ENABLED, true);
    args.put(FRONTEND_READINESS_CHECK_INTERVAL_SECONDS, readinessCheckInterval);

    args.put(FRONTEND_HEADER_PROPAGATION_ENABLED, true);
    args.put(
        FRONTEND_HEADER_PROPAGATION_ALLOWED,
        "trace-id, span-id, parent-span-id, trace-sampled, trace-reported");

    args.put(KAFKA_PRODUCER_METADATA_MAX_AGE, metadataMaxAge);

    args.put(FRONTEND_FORCE_TOPIC_MAX_MESSAGE_SIZE, true);
    args.put(FRONTEND_IDLE_TIMEOUT, Duration.ofSeconds(2));

    args.put(FRONTEND_THROUGHPUT_TYPE, "fixed");
    args.put(FRONTEND_THROUGHPUT_FIXED_MAX, 50 * 1024L);

    args.put(FRONTEND_GRACEFUL_SHUTDOWN_ENABLED, false);

    args.put(METRICS_MICROMETER_REPORT_PERIOD, Duration.ofSeconds(1));

    args.put(FRONTEND_MESSAGE_PREVIEW_ENABLED, true);
    args.put(FRONTEND_MESSAGE_PREVIEW_LOG_PERSIST_PERIOD, Duration.ofSeconds(1));

    args.put(AUTH_USERNAME, "username");
    args.put(AUTH_PASSWORD, "password");

    args.putAll(extraArgs);

    return args.entrySet().stream().map(e -> getArgument(e.getKey(), e.getValue())).toList();
  }

  @Override
  public HermesTestApp start() {
    app = new SpringApplicationBuilder(HermesFrontend.class).web(WebApplicationType.NONE);
    currentArgs = createArgs();
    app.run(currentArgs.toArray(new String[0]));
    port = app.context().getBean(HermesServer.class).getPort();
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
      throw new IllegalStateException("hermes-frontend port hasn't been initialized");
    }
    return port;
  }

  public int getSSLPort() {
    return app.context().getBean(HermesServer.class).getSSLPort();
  }

  public <T> T getBean(Class<T> clazz) {
    return app.context().getBean(clazz);
  }

  public HermesFrontendTestApp metadataMaxAgeInSeconds(int value) {
    metadataMaxAge = Duration.ofSeconds(value);
    return this;
  }

  public HermesFrontendTestApp readinessCheckIntervalInSeconds(int value) {
    readinessCheckInterval = Duration.ofSeconds(value);
    return this;
  }

  public HermesFrontendTestApp kafkaCheckEnabled() {
    kafkaCheckEnabled = true;
    return this;
  }

  public HermesFrontendTestApp kafkaCheckDisabled() {
    kafkaCheckEnabled = false;
    return this;
  }

  private static String getArgument(String config, Object value) {
    return "--" + config + "=" + value;
  }

  @Override
  public boolean shouldBeRestarted() {
    List<String> args = createArgs();
    return !args.equals(currentArgs);
  }

  @Override
  public void restoreDefaultSettings() {
    extraArgs.clear();
    profiles.clear();
    profiles.add("integration");
  }
}
