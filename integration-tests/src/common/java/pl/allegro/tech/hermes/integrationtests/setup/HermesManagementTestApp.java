package pl.allegro.tech.hermes.integrationtests.setup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.waitAtMost;
import static pl.allegro.tech.hermes.infrastructure.dc.DefaultDatacenterNameProvider.DEFAULT_DC_NAME;
import static pl.allegro.tech.hermes.test.helper.endpoint.TimeoutAdjuster.adjust;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.core.env.Environment;
import pl.allegro.tech.hermes.integrationtests.prometheus.PrometheusExtension;
import pl.allegro.tech.hermes.management.HermesManagement;
import pl.allegro.tech.hermes.management.domain.group.GroupService;
import pl.allegro.tech.hermes.management.domain.subscription.SubscriptionService;
import pl.allegro.tech.hermes.management.domain.topic.TopicService;
import pl.allegro.tech.hermes.test.helper.containers.ConfluentSchemaRegistryContainer;
import pl.allegro.tech.hermes.test.helper.containers.KafkaContainerCluster;
import pl.allegro.tech.hermes.test.helper.containers.ZookeeperContainer;
import pl.allegro.tech.hermes.test.helper.environment.HermesTestApp;

public class HermesManagementTestApp implements HermesTestApp {

  private int port = -1;

  private int auditEventPort = -1;

  public static String AUDIT_EVENT_PATH = "/audit-events";

  private final Map<String, ZookeeperContainer> hermesZookeepers;
  private final Map<String, KafkaContainerCluster> kafkaClusters;
  private final ConfluentSchemaRegistryContainer schemaRegistry;
  private SpringApplicationBuilder app = null;
  private List<String> currentArgs = List.of();
  private PrometheusExtension prometheus = null;
  private final List<String> extraArgs = new ArrayList<>();

  public HermesManagementTestApp(
      ZookeeperContainer hermesZookeeper,
      KafkaContainerCluster kafka,
      ConfluentSchemaRegistryContainer schemaRegistry) {
    this(Map.of(DEFAULT_DC_NAME, hermesZookeeper), Map.of(DEFAULT_DC_NAME, kafka), schemaRegistry);
  }

  public HermesManagementTestApp(
      Map<String, ZookeeperContainer> hermesZookeepers,
      Map<String, KafkaContainerCluster> kafkaClusters,
      ConfluentSchemaRegistryContainer schemaRegistry) {
    this.hermesZookeepers = hermesZookeepers;
    this.kafkaClusters = kafkaClusters;
    this.schemaRegistry = schemaRegistry;
  }

  @Override
  public HermesTestApp start() {
    currentArgs = createArgs();
    app = new SpringApplicationBuilder(HermesManagement.class);
    app.run(currentArgs.toArray(new String[0]));
    String localServerPort =
        app.context().getBean(Environment.class).getProperty("local.server.port");
    if (localServerPort == null) {
      throw new IllegalStateException("Cannot get hermes-management port");
    }
    port = Integer.parseInt(localServerPort);
    waitUntilReady();
    return this;
  }

  private void waitUntilReady() {
    try {
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(new URI("http://localhost:" + getPort() + "/mode"))
              .GET()
              .build();
      HttpClient httpClient = HttpClient.newHttpClient();

      waitAtMost(adjust(240), TimeUnit.SECONDS)
          .untilAsserted(
              () -> {
                try {
                  HttpResponse<String> response =
                      httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                  assertThat(response.body()).isEqualTo("readWrite");
                } catch (IOException | InterruptedException e) {
                  throw new AssertionError("Reading management mode failed", e);
                }
              });
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public int getPort() {
    if (port == -1) {
      throw new IllegalStateException("hermes-management port hasn't been initialized");
    }
    return port;
  }

  @Override
  public void stop() {
    if (app != null) {
      app.context().close();
      app = null;
    }
  }

  public void addEventAuditorListener(int port) {
    auditEventPort = port;
  }

  void withPrometheus(PrometheusExtension prometheus) {
    this.prometheus = prometheus;
  }

  @Override
  public void restoreDefaultSettings() {
    prometheus = null;
  }

  @Override
  public boolean shouldBeRestarted() {
    List<String> args = createArgs();
    return !args.equals(currentArgs);
  }

  private List<String> createArgs() {
    List<String> args = new ArrayList<>();
    args.add("--spring.profiles.active=integration");
    args.add("--server.port=0");
    args.add("--prometheus.client.enabled=true");
    args.add("--prometheus.client.socketTimeoutMillis=500");
    if (prometheus != null) {
      args.add("--prometheus.client.externalMonitoringUrl=" + prometheus.getEndpoint());
      args.add("--prometheus.client.cacheTtlSeconds=0");
    }
    args.add("--topic.partitions=2");
    args.add("--topic.uncleanLeaderElectionEnabled=false");
    int smallestClusterSize =
        kafkaClusters.values().stream()
            .map(cluster -> cluster.getAllBrokers().size())
            .min(Integer::compareTo)
            .orElse(1);
    args.add("--topic.replicationFactor=" + smallestClusterSize);
    int idx = 0;
    for (Map.Entry<String, ZookeeperContainer> zk : hermesZookeepers.entrySet()) {
      args.add("--storage.clusters[" + idx + "].datacenter=" + zk.getKey());
      args.add("--storage.clusters[" + idx + "].clusterName=zk");
      args.add(
          "--storage.clusters["
              + idx
              + "].connectionString="
              + zk.getValue().getConnectionString());
      idx++;
    }
    idx = 0;
    for (Map.Entry<String, KafkaContainerCluster> kafka : kafkaClusters.entrySet()) {
      args.add("--kafka.clusters[" + idx + "].datacenter=" + kafka.getKey());
      args.add("--kafka.clusters[" + idx + "].clusterName=primary");
      args.add(
          "--kafka.clusters["
              + idx
              + "].bootstrapKafkaServer="
              + kafka.getValue().getBootstrapServersForExternalClients());
      args.add("--kafka.clusters[" + idx + "].namespace=itTest");
      idx++;
    }

    args.add("--schema.repository.serverUrl=" + schemaRegistry.getUrl());
    args.add("--topic.touchSchedulerEnabled=" + false);
    args.add("--topic.allowRemoval=" + true);
    args.add("--topic.allowedTopicLabels=" + "label-1, label-2, label-3");
    if (auditEventPort != -1) {
      args.add("--audit.isEventAuditEnabled=" + true);
      args.add("--audit.eventUrl=" + "http://localhost:" + auditEventPort + AUDIT_EVENT_PATH);
    }

    args.add("--topic.removeSchema=" + true);
    args.add("--storage.pathPrefix=" + "/hermes");
    args.add("--subscription.subscribersWithAccessToAnyTopic[0].ownerSource=" + "Plaintext");
    args.add(
        "--subscription.subscribersWithAccessToAnyTopic[0].ownerId="
            + "subscriberAllowedToAccessAnyTopic");
    args.add("--subscription.subscribersWithAccessToAnyTopic[0].protocols=" + "http, https");
    args.add("--group.allowedGroupNameRegex=" + "[a-zA-Z0-9_.-]+");
    args.add("--group.nonAdminCreationEnabled=" + true);
    args.add("--schema.repository.type=schema_registry");
    args.add("--schema.repository.deleteSchemaPathSuffix=");

    args.addAll(extraArgs);

    return args;
  }

  public SubscriptionService subscriptionService() {
    return app.context().getBean(SubscriptionService.class);
  }

  public TopicService topicService() {
    return app.context().getBean(TopicService.class);
  }

  public GroupService groupService() {
    return app.context().getBean(GroupService.class);
  }

  public void withArgs(List<String> args) {
    extraArgs.addAll(args);
  }
}
