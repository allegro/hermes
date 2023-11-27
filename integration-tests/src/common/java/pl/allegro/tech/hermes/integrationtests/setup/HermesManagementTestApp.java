package pl.allegro.tech.hermes.integrationtests.setup;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.core.env.Environment;
import pl.allegro.tech.hermes.management.HermesManagement;
import pl.allegro.tech.hermes.test.helper.containers.KafkaContainerCluster;
import pl.allegro.tech.hermes.test.helper.containers.ZookeeperContainer;

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

import static com.jayway.awaitility.Awaitility.waitAtMost;
import static pl.allegro.tech.hermes.infrastructure.dc.DefaultDatacenterNameProvider.DEFAULT_DC_NAME;
import static pl.allegro.tech.hermes.test.helper.endpoint.TimeoutAdjuster.adjust;

public class HermesManagementTestApp implements HermesTestApp {

    private int port = -1;
    private final Map<String, ZookeeperContainer> hermesZookeepers;
    private final Map<String, KafkaContainerCluster> kafkaClusters;
    private final SpringApplicationBuilder app = new SpringApplicationBuilder(HermesManagement.class);

    public HermesManagementTestApp(ZookeeperContainer hermesZookeeper, KafkaContainerCluster kafka) {
        this(Map.of(DEFAULT_DC_NAME, hermesZookeeper), Map.of(DEFAULT_DC_NAME, kafka));
    }

    public HermesManagementTestApp(Map<String, ZookeeperContainer> hermesZookeepers,
                                   Map<String, KafkaContainerCluster> kafkaClusters) {
        this.hermesZookeepers = hermesZookeepers;
        this.kafkaClusters = kafkaClusters;
    }

    @Override
    public HermesTestApp start() {
        List<String> args = new ArrayList<>();
        args.add("--server.port=0");
        args.add("--prometheus.client.enabled=true");
        args.add("--topic.partitions=2");
        args.add("--topic.uncleanLeaderElectionEnabled=false");
        int smallestClusterSize =  kafkaClusters.values().stream()
                .map(cluster -> cluster.getAllBrokers().size())
                .min(Integer::compareTo)
                .orElse(1);
        args.add("--topic.replicationFactor=" + smallestClusterSize);
        int idx = 0;
        for (Map.Entry<String, ZookeeperContainer> zk : hermesZookeepers.entrySet()) {
            args.add("--storage.clusters[" + idx + "].datacenter=" + zk.getKey());
            args.add("--storage.clusters[" + idx + "].clusterName=zk");
            args.add("--storage.clusters[" + idx + "].connectionString=" + zk.getValue().getConnectionString());
            idx++;
        }
        idx = 0;
        for (Map.Entry<String, KafkaContainerCluster> kafka : kafkaClusters.entrySet()) {
            args.add("--kafka.clusters[" + idx + "].datacenter=" + kafka.getKey());
            args.add("--kafka.clusters[" + idx + "].clusterName=primary");
            args.add("--kafka.clusters[" + idx + "].bootstrapKafkaServer=" + kafka.getValue().getBootstrapServersForExternalClients());
            args.add("--kafka.clusters[" + idx + "].namespace=itTest");
            idx++;
        }
        app.run(args.toArray(new String[0]));
        String localServerPort = app.context().getBean(Environment.class).getProperty("local.server.port");
        if (localServerPort == null) {
            throw new IllegalStateException("Cannot get hermes-management port");
        }
        port = Integer.parseInt(localServerPort);
        waitUntilReady();
        return this;
    }

    private void waitUntilReady() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:" + getPort() + "/mode"))
                    .GET()
                    .build();
            HttpClient httpClient = HttpClient.newHttpClient();

            waitAtMost(adjust(240), TimeUnit.SECONDS).until(() -> {
                try {
                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    if (!"readWrite".equals(response.body())) {
                        throw new RuntimeException();
                    }
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
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
        app.context().close();
    }
}
