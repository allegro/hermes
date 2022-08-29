package pl.allegro.tech.hermes.test.helper.containers;

import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.containers.ToxiproxyContainer.ContainerProxy;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;
import static org.rnorth.ducttape.unreliables.Unreliables.retryUntilTrue;
import static pl.allegro.tech.hermes.test.helper.containers.TestcontainersUtils.copyScriptToContainer;
import static pl.allegro.tech.hermes.test.helper.containers.TestcontainersUtils.readFileFromClasspath;

public class KafkaContainerCluster implements Startable {
    private static final String CONFLUENT_PLATFORM_VERSION = "6.1.0";
    private static final DockerImageName ZOOKEEPER_IMAGE_NAME = DockerImageName.parse("confluentinc/cp-zookeeper")
            .withTag(CONFLUENT_PLATFORM_VERSION);
    private static final DockerImageName KAFKA_IMAGE_NAME = DockerImageName.parse("confluentinc/cp-kafka")
            .withTag(CONFLUENT_PLATFORM_VERSION);
    private static final DockerImageName TOXIPROXY_IMAGE_NAME = DockerImageName.parse("shopify/toxiproxy")
            .withTag("2.1.0");

    private static final Duration CLUSTER_START_TIMEOUT = Duration.ofMinutes(360);
    private static final String ZOOKEEPER_NETWORK_ALIAS = "zookeeper";
    private static final String READINESS_CHECK_SCRIPT = "/kafka_readiness_check.sh";
    private static final int ZOOKEEPER_PORT = 2181;

    private final List<ContainerProxy> proxies = new ArrayList<>();
    private final int brokersNum;
    private final int minInSyncReplicas;
    private final ZookeeperContainer zookeeper;
    private final ToxiproxyContainer toxiproxy;
    private final List<KafkaContainer> brokers = new ArrayList<>();

    public KafkaContainerCluster(int brokersNum) {
        checkArgument(brokersNum > 0, "brokersNum '" + brokersNum + "' must be greater than 0");
        this.brokersNum = brokersNum;
        this.minInSyncReplicas = Math.max(brokersNum - 1, 1);
        this.zookeeper = createZookeeper(Network.newNetwork());
        this.toxiproxy = new ToxiproxyContainer(TOXIPROXY_IMAGE_NAME)
                .withNetwork(zookeeper.getNetwork());
        int internalTopicsRf = Math.max(brokersNum - 1, 1);
        for (int brokerId = 0; brokerId < brokersNum; brokerId++) {
            KafkaContainer container = new KafkaContainer(KAFKA_IMAGE_NAME, zookeeper.getNetwork(), brokerId)
                    .dependsOn(zookeeper)
                    .withExternalZookeeper(ZOOKEEPER_NETWORK_ALIAS + ":" + ZOOKEEPER_PORT)
                    .withEnv("KAFKA_BROKER_ID", brokerId + "")
                    .withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", internalTopicsRf + "")
                    .withEnv("KAFKA_OFFSETS_TOPIC_NUM_PARTITIONS", internalTopicsRf + "")
                    .withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", internalTopicsRf + "")
                    .withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", internalTopicsRf + "")
                    .withEnv("KAFKA_MIN_INSYNC_REPLICAS", minInSyncReplicas + "");
            brokers.add(container);
        }
    }

    private ZookeeperContainer createZookeeper(Network network) {
        return new ZookeeperContainer(ZOOKEEPER_IMAGE_NAME, ZOOKEEPER_PORT)
                .withNetwork(network)
                .withNetworkAliases(ZOOKEEPER_NETWORK_ALIAS);
    }

    public List<BrokerId> getAllBrokers() {
        return brokers.stream().map(KafkaContainer::getBrokerId).collect(toList());
    }

    public int getMinInSyncReplicas() {
        return minInSyncReplicas;
    }

    public String getBootstrapServersForExternalClients() {
        return brokers.stream()
                .map(KafkaContainer::getAddressForExternalClients)
                .collect(Collectors.joining(","));
    }

    public String getBootstrapServersForInternalClients() {
        return brokers.stream()
                .map(KafkaContainer::getAddressForInternalClients)
                .collect(Collectors.joining(","));
    }

    public Network getNetwork() {
        return zookeeper.getNetwork();
    }

    @Override
    public void start() {
        try {
            startToxiproxy();
            Startables.deepStart(brokers)
                    .get(CLUSTER_START_TIMEOUT.getSeconds(), SECONDS);
            String readinessScript = readFileFromClasspath("testcontainers/kafka_readiness_check.sh");
            for (KafkaContainer kafkaContainer : brokers) {
                copyScriptToContainer(readinessScript, kafkaContainer, READINESS_CHECK_SCRIPT);
            }
            waitForClusterFormation();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private void startToxiproxy() {
        toxiproxy.start();
        for (KafkaContainer kafkaContainer : brokers) {
            ContainerProxy proxy = toxiproxy.getProxy(kafkaContainer, kafkaContainer.getExposedPorts().get(0));
            proxies.add(proxy);
            kafkaContainer.withAdvertisedPort(proxy.getProxyPort());
        }
    }

    public void stop(List<BrokerId> brokerIds) {
        select(brokerIds).stream()
                .parallel()
                .forEach(KafkaContainer::stopKafka);
    }

    public void start(List<BrokerId> brokerIds) {
        select(brokerIds).stream()
                .parallel()
                .forEach(KafkaContainer::startKafka);
    }

    private Set<KafkaContainer> select(List<BrokerId> brokerIds) {
        return brokers.stream()
                .filter(b -> brokerIds.contains(b.getBrokerId()))
                .collect(Collectors.toSet());
    }

    @Override
    public void stop() {
        Stream.concat(brokers.stream(), Stream.of(zookeeper))
                .parallel()
                .forEach(GenericContainer::stop);
    }

    public void cutOffConnectionsBetweenBrokersAndClients() {
        proxies.forEach(proxy -> proxy.setConnectionCut(true));
    }

    public void restoreConnectionsBetweenBrokersAndClients() {
        proxies.forEach(proxy -> proxy.setConnectionCut(false));
    }

    public void makeClusterOperational() {
        brokers.stream()
                .filter(broker -> !broker.isKafkaRunning())
                .parallel()
                .forEach(KafkaContainer::startKafka);
        waitForClusterFormation();
    }

    private void waitForClusterFormation() {
        retryUntilTrue((int) CLUSTER_START_TIMEOUT.getSeconds(), TimeUnit.SECONDS, this::isZookeeperReady);
        retryUntilTrue((int) CLUSTER_START_TIMEOUT.getSeconds(), TimeUnit.SECONDS, this::isKafkaReady);
    }

    private boolean isZookeeperReady() throws IOException, InterruptedException {
        ExecResult result = zookeeper.execInContainer(
                "sh",
                "-c",
                "zookeeper-shell " + ZOOKEEPER_NETWORK_ALIAS + ":" + ZOOKEEPER_PORT + " ls /brokers/ids | tail -n 1"
        );
        String brokers = result.getStdout();
        return brokers != null && brokers.split(",").length == brokersNum;
    }

    private boolean isKafkaReady() throws IOException, InterruptedException {
        KafkaContainer firstBroker = selectFirstRunningBroker();
        ExecResult result = firstBroker.execInContainer(
                "sh",
                "-c",
                READINESS_CHECK_SCRIPT + " " + brokersNum
        );
        return result.getExitCode() == 0;
    }

    public int countOfflinePartitions() throws IOException, InterruptedException {
        return countPartitions("--unavailable-partitions");
    }

    public int countUnderReplicatedPartitions() throws IOException, InterruptedException {
        return countPartitions("--under-replicated-partitions");
    }

    private int countPartitions(String option) throws IOException, InterruptedException {
        KafkaContainer firstBroker = selectFirstRunningBroker();
        ExecResult result = firstBroker.execInContainer(
                "sh",
                "-c",
                "kafka-topics --bootstrap-server localhost:9092 --describe " + option + " | wc -l"
        );
        String sanitizedOutput = result.getStdout()
                .replaceAll("\"", "")
                .replaceAll("\\s+", "");
        return Integer.parseInt(sanitizedOutput);
    }

    private KafkaContainer selectFirstRunningBroker() {
        return brokers.stream()
                .filter(KafkaContainer::isKafkaRunning)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("There is no running broker"));
    }
}
