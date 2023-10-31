package pl.allegro.tech.hermes.integrationtests.setup;

import com.google.common.collect.ImmutableMap;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import pl.allegro.tech.hermes.management.HermesManagement;
import pl.allegro.tech.hermes.test.helper.endpoint.BrokerOperations;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesAPIOperations;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesEndpoints;
import pl.allegro.tech.hermes.test.helper.util.Ports;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.jayway.awaitility.Awaitility.waitAtMost;
import static pl.allegro.tech.hermes.test.helper.endpoint.TimeoutAdjuster.adjust;

public class HermesManagementExtension implements BeforeAllCallback {
    private static final String KAFKA_NAMESPACE = "itTest";

    private int port;
    private final List<HermesManagementExtension.ClusterInfo> kafkaClusters = new ArrayList<>();
    private final List<HermesManagementExtension.ClusterInfo> zkClusters = new ArrayList<>();
    private final int replicationFactor;
    private boolean uncleanLeaderElectionEnabled;
    private String schemaRegistry;
    private boolean avroContentTypeMetadataRequired;
    private boolean graphiteExternalMetricsStorage;

    private HermesManagementExtension(int port, int replicationFactor, boolean uncleanLeaderElectionEnabled, String schemaRegistry, boolean avroContentTypeMetadataRequired, boolean graphiteExternalMetricsStorage) {
        this.port = port;
        this.replicationFactor = replicationFactor;
        this.uncleanLeaderElectionEnabled = uncleanLeaderElectionEnabled;
        this.schemaRegistry = schemaRegistry;
        this.avroContentTypeMetadataRequired = avroContentTypeMetadataRequired;
        this.graphiteExternalMetricsStorage = graphiteExternalMetricsStorage;
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        try {
            startManagement();
            HermesAPIOperations operations = setupOperations(startZookeeperClient());
            waitUntilManagementIsInReadWriteMode(operations);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void startManagement() {
        List<String> args = new ArrayList<>();
        args.add("--server.port=" + port);
        args.add("--spring.profiles.active=integration");
        int kafkaClusterIndex = 0;
        for (HermesManagementExtension.ClusterInfo kafkaCluster : kafkaClusters) {
            args.add("--kafka.clusters[" + kafkaClusterIndex + "].datacenter=" + kafkaCluster.getDc());
            args.add("--kafka.clusters[" + kafkaClusterIndex + "].clusterName=primary");
            args.add("--kafka.clusters[" + kafkaClusterIndex + "].bootstrapKafkaServer=" + kafkaCluster.getConnectionString());
            args.add("--kafka.clusters[" + kafkaClusterIndex + "].namespace=" + KAFKA_NAMESPACE);
            kafkaClusterIndex++;
        }
        int zkClusterIndex = 0;
        for (HermesManagementExtension.ClusterInfo zkCluster : zkClusters) {
            args.add("--storage.clusters[" + zkClusterIndex + "].datacenter=" + zkCluster.getDc());
            args.add("--storage.clusters[" + zkClusterIndex + "].clusterName=zk");
            args.add("--storage.clusters[" + zkClusterIndex + "].connectionString=" + zkCluster.getConnectionString());
            zkClusterIndex++;
        }
        args.add("--topic.replicationFactor=" + replicationFactor);
        args.add("--topic.uncleanLeaderElectionEnabled=" + uncleanLeaderElectionEnabled);
        args.add("--topic.avroContentTypeMetadataRequired=" + avroContentTypeMetadataRequired);
        args.add("--schema.repository.serverUrl=" + schemaRegistry);

        args.add("--graphite.client.enabled=" + graphiteExternalMetricsStorage);
        args.add("--prometheus.client.enabled=" + !graphiteExternalMetricsStorage);
        HermesManagement.main(args.toArray(new String[0]));
    }

    private List<CuratorFramework> startSeparateZookeeperClientPerCluster() {
        final List<CuratorFramework> zookeeperClients =
                zkClusters.stream()
                        .map(HermesManagementExtension.ClusterInfo::getConnectionString)
                        .map(this::buildCuratorFramework)
                        .collect(Collectors.toList());
        zookeeperClients.forEach(CuratorFramework::start);
        return zookeeperClients;
    }

    private CuratorFramework buildCuratorFramework(String connectString) {
        return CuratorFrameworkFactory.builder()
                .connectString(connectString)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .build();
    }

    private void waitUntilManagementIsInReadWriteMode(HermesAPIOperations operations) {
        waitAtMost(adjust(240), TimeUnit.SECONDS).until(operations::isInReadWriteMode);
    }

    private HermesAPIOperations setupOperations(CuratorFramework zookeeper) {
        BrokerOperations brokerOperations = new BrokerOperations(ImmutableMap.of(), "");
        String managementUrl = "http://localhost:" + port + "/";
        HermesEndpoints management = new HermesEndpoints(managementUrl, managementUrl);
        return new HermesAPIOperations(management, null);
    }

    private CuratorFramework startZookeeperClient() {
        final CuratorFramework zookeeperClient = buildCuratorFramework(zkClusters.get(0).getConnectionString());
        zookeeperClient.start();
        return zookeeperClient;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int port = Ports.nextAvailable();
        private final List<HermesManagementExtension.ClusterInfo> kafkaClusters = new ArrayList<>();
        private final List<HermesManagementExtension.ClusterInfo> zkClusters = new ArrayList<>();
        private int replicationFactor = 1;
        private boolean uncleanLeaderElectionEnabled = false;
        private String schemaRegistry;
        private boolean avroContentTypeMetadataRequired = true;
        private boolean graphiteExternalMetricsStorage = false;

        public HermesManagementExtension.Builder port(int port) {
            this.port = port;
            return this;
        }

        public HermesManagementExtension.Builder schemaRegistry(String schemaRegistry) {
            this.schemaRegistry = schemaRegistry;
            return this;
        }


        public HermesManagementExtension.Builder replicationFactor(int replicationFactor) {
            this.replicationFactor = replicationFactor;
            return this;
        }

        public HermesManagementExtension.Builder uncleanLeaderElectionEnabled(boolean enabled) {
            this.uncleanLeaderElectionEnabled = enabled;
            return this;
        }

        public HermesManagementExtension.Builder addZookeeperCluster(String dc, String connectionString) {
            zkClusters.add(new ClusterInfo(dc, connectionString));
            return this;
        }

        public HermesManagementExtension.Builder addKafkaCluster(String dc, String connectionString) {
            kafkaClusters.add(new ClusterInfo(dc, connectionString));
            return this;
        }

        public HermesManagementExtension.Builder withGraphiteExternalStorageEnabled() {
            this.graphiteExternalMetricsStorage = true;
            return this;
        }

        public HermesManagementExtension build() {
            return new HermesManagementExtension(this.port,
                    this.replicationFactor,
                    this.uncleanLeaderElectionEnabled,
                    this.schemaRegistry,
                    this.avroContentTypeMetadataRequired,
                    this.graphiteExternalMetricsStorage);
        }
    }

    private static class ClusterInfo {
        private final String dc;
        private final String connectionString;

        private ClusterInfo(String dc, String connectionString) {
            this.dc = dc;
            this.connectionString = connectionString;
        }

        public String getDc() {
            return dc;
        }

        public String getConnectionString() {
            return connectionString;
        }
    }
}
