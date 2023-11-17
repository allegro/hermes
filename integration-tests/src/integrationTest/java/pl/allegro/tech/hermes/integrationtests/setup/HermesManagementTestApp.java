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
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.waitAtMost;
import static pl.allegro.tech.hermes.test.helper.endpoint.TimeoutAdjuster.adjust;

public class HermesManagementTestApp implements HermesTestApp {

    private int port = -1;
    private final ZookeeperContainer hermesZookeeper;
    private final KafkaContainerCluster kafka;
    private final SpringApplicationBuilder app = new SpringApplicationBuilder(HermesManagement.class);

    public HermesManagementTestApp(ZookeeperContainer hermesZookeeper, KafkaContainerCluster kafka) {
        this.hermesZookeeper = hermesZookeeper;
        this.kafka = kafka;
    }

    @Override
    public HermesTestApp start() {
        app.run(
                "--server.port=0",
                "--storage.clusters[0].datacenter=dc",
                "--storage.clusters[0].clusterName=zk",
                "--storage.clusters[0].connectionString=" + hermesZookeeper.getConnectionString(),
                "--prometheus.client.enabled=true",
                "--kafka.clusters[0].datacenter=dc",
                "--kafka.clusters[0].clusterName=primary",
                "--kafka.clusters[0].bootstrapKafkaServer=" + kafka.getBootstrapServersForExternalClients(),
                "--kafka.clusters[0].namespace=itTest",
                "--topic.replicationFactor=" + kafka.getAllBrokers().size(),
                "--topic.uncleanLeaderElectionEnabled=false"
        );
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
