package pl.allegro.tech.hermes.integration.setup;

import com.google.common.collect.ImmutableMap;
import com.netflix.config.DynamicPropertyFactory;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.integration.env.ManagementStarter;
import pl.allegro.tech.hermes.integration.helper.Waiter;
import pl.allegro.tech.hermes.test.helper.endpoint.BrokerOperations;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesAPIOperations;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesEndpoints;
import pl.allegro.tech.hermes.test.helper.util.Ports;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HermesManagementInstance {
    private final HermesAPIOperations operations;
    private final ManagementStarter managementStarter;

    private HermesManagementInstance(HermesAPIOperations operations, ManagementStarter managementStarter) {
        this.operations = operations;
        this.managementStarter = managementStarter;
    }

    public HermesAPIOperations operations() {
        return operations;
    }

    public static Starter starter() {
        return new Starter();
    }

    public void stop() {
        try {
            managementStarter.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class Starter {
        private static final String KAFKA_NAMESPACE = "itTest";

        private final int port = Ports.nextAvailable();
        private final Map<String, String> kafkaClusters = new HashMap<>();
        private final Map<String, String> zkClusters = new HashMap<>();
        private int replicationFactor = 1;
        private boolean uncleanLeaderElectionEnabled = false;

        public Starter replicationFactor(int replicationFactor) {
            this.replicationFactor = replicationFactor;
            return this;
        }

        public Starter uncleanLeaderElectionEnabled(boolean enabled) {
            this.uncleanLeaderElectionEnabled = enabled;
            return this;
        }

        public Starter addZookeeperCluster(String dc, String connectionString) {
            zkClusters.put(dc, connectionString);
            return this;
        }

        public Starter addKafkaCluster(String dc, String connectionString) {
            kafkaClusters.put(dc, connectionString);
            return this;
        }

        public HermesManagementInstance start() {
            try {
                ManagementStarter managementStarter = startManagement();
                HermesAPIOperations operations = setupOperations();
                return new HermesManagementInstance(operations, managementStarter);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private ManagementStarter startManagement() throws Exception {
            List<String> args = new ArrayList<>();
            int kafkaClusterIndex = 0;
            for (Map.Entry<String, String> kafkaCluster : kafkaClusters.entrySet()) {
                args.add("--kafka.clusters[" + kafkaClusterIndex + "].datacenter=" + kafkaCluster.getKey());
                args.add("--kafka.clusters[" + kafkaClusterIndex + "].clusterName=" + kafkaCluster.getKey());
                args.add("--kafka.clusters[" + kafkaClusterIndex + "].bootstrapKafkaServer=" + kafkaCluster.getValue());
                args.add("--kafka.clusters[" + kafkaClusterIndex + "].clusterName=10000");
                args.add("--kafka.clusters[" + kafkaClusterIndex + "].namespace=" + KAFKA_NAMESPACE);
                kafkaClusterIndex++;
            }
            int zkClusterIndex = 0;
            for (Map.Entry<String, String> zkCluster : zkClusters.entrySet()) {
                args.add("--storage.clusters[" + zkClusterIndex + "].datacenter=" + zkCluster.getKey());
                args.add("--storage.clusters[" + zkClusterIndex + "].clusterName=" + zkCluster.getKey());
                args.add("--storage.clusters[" + zkClusterIndex + "].connectionString=" + zkCluster.getValue());
                zkClusterIndex++;
            }
            args.add("--topic.replicationFactor=" + replicationFactor);
            args.add("--topic.uncleanLeaderElectionEnabled=" + uncleanLeaderElectionEnabled);
            args.add("--audit.isEventAuditEnabled=false");
            ManagementStarter managementStarter = new ManagementStarter(port, "integration", args.toArray(new String[0]));
            managementStarter.start();
            return managementStarter;
        }

        private HermesAPIOperations setupOperations() {
            ConfigFactory configFactory = new ConfigFactory(DynamicPropertyFactory.getInstance());
            BrokerOperations brokerOperations = new BrokerOperations(ImmutableMap.of(), configFactory);
            String managementUrl = "http://localhost:" + port + "/";
            HermesEndpoints management = new HermesEndpoints(managementUrl, managementUrl);
            Waiter wait = new Waiter(management, startZookeeperClient(), brokerOperations, null, KAFKA_NAMESPACE);
            return new HermesAPIOperations(management, wait);
        }

        private CuratorFramework startZookeeperClient() {
            final CuratorFramework zookeeperClient = CuratorFrameworkFactory.builder()
                    .connectString(zkClusters.values().iterator().next())
                    .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                    .build();
            zookeeperClient.start();
            return zookeeperClient;
        }
    }
}
