package pl.allegro.tech.hermes.integrationtests.setup;

import com.google.common.collect.ImmutableMap;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
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

// TODO check if it is possible to use another approach for starting instances
public class HermesManagementInstance {

    private final HermesAPIOperations operations;

    private HermesManagementInstance(HermesAPIOperations operations) {
        this.operations = operations;
    }

    public HermesAPIOperations operations() {
        return operations;
    }

    public static Starter starter() {
        return new Starter();
    }

    public static class Starter {
        private static final String KAFKA_NAMESPACE = "itTest";

        private int port = Ports.nextAvailable();
        private final List<ClusterInfo> kafkaClusters = new ArrayList<>();
        private final List<ClusterInfo> zkClusters = new ArrayList<>();
        private int replicationFactor = 1;
        private boolean uncleanLeaderElectionEnabled = false;
        private String schemaRegistry;
        private boolean avroContentTypeMetadataRequired = true;
        private boolean graphiteExternalMetricsStorage = false;

        public Starter port(int port) {
            this.port = port;
            return this;
        }

        public Starter schemaRegistry(String schemaRegistry) {
            this.schemaRegistry = schemaRegistry;
            return this;
        }


        public Starter replicationFactor(int replicationFactor) {
            this.replicationFactor = replicationFactor;
            return this;
        }

        public Starter uncleanLeaderElectionEnabled(boolean enabled) {
            this.uncleanLeaderElectionEnabled = enabled;
            return this;
        }

        public Starter addZookeeperCluster(String dc, String connectionString) {
            zkClusters.add(new ClusterInfo(dc, connectionString));
            return this;
        }

        public Starter addKafkaCluster(String dc, String connectionString) {
            kafkaClusters.add(new ClusterInfo(dc, connectionString));
            return this;
        }

        public Starter withGraphiteExternalStorageEnabled() {
            this.graphiteExternalMetricsStorage = true;
            return this;
        }

        public HermesManagementInstance start() {
            try {
                startManagement();
                HermesAPIOperations operations = setupOperations(startZookeeperClient());
                waitUntilManagementIsInReadWriteMode(operations);
                return new HermesManagementInstance(operations);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private void waitUntilManagementIsInReadWriteMode(HermesAPIOperations operations) {
            waitAtMost(adjust(240), TimeUnit.SECONDS).until(operations::isInReadWriteMode);
        }

        private void startManagement() {
            List<String> args = new ArrayList<>();
            args.add("--server.port=" + port);
            args.add("--spring.profiles.active=integration");
            int kafkaClusterIndex = 0;
            for (ClusterInfo kafkaCluster : kafkaClusters) {
                args.add("--kafka.clusters[" + kafkaClusterIndex + "].datacenter=" + kafkaCluster.getDc());
                args.add("--kafka.clusters[" + kafkaClusterIndex + "].clusterName=primary");
                args.add("--kafka.clusters[" + kafkaClusterIndex + "].bootstrapKafkaServer=" + kafkaCluster.getConnectionString());
                args.add("--kafka.clusters[" + kafkaClusterIndex + "].namespace=" + KAFKA_NAMESPACE);
                kafkaClusterIndex++;
            }
            int zkClusterIndex = 0;
            for (ClusterInfo zkCluster : zkClusters) {
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

        private List<CuratorFramework> startSeparateZookeeperClientPerCluster() {
            final List<CuratorFramework> zookeeperClients =
                    zkClusters.stream()
                            .map(ClusterInfo::getConnectionString)
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
