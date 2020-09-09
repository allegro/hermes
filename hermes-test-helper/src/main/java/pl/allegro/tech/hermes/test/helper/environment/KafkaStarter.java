package pl.allegro.tech.hermes.test.helper.environment;

import kafka.server.KafkaServerStartable;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

public class KafkaStarter implements Starter<KafkaServerStartable> {

    private static final Logger logger = LoggerFactory.getLogger(KafkaStarter.class);

    private boolean running;
    private final FixedHostPortGenericContainer kafkaContainer;
    private final int zkPort;
    private final String basePath;

    public KafkaStarter(Network network, int zkPort, String basePath, int port, int saslPort, String hostname) {
        this.zkPort = zkPort;
        this.basePath = basePath;
        this.kafkaContainer = new FixedHostPortGenericContainer<>("confluentinc/cp-kafka")
                .withNetwork(network)
                .withCreateContainerCmdModifier(it -> it.withName(hostname))
//                .withExposedPorts(9092)
                .withFixedExposedPort(port, 9092)
//                .withExposedPorts(9092, 9093)
//                .withFixedExposedPort(saslPort, 9092)
//                .withEnv("KAFKA_SASL_MECHANISM", "PLAIN")
//                .withEnv("KAFKA_SECURITY_PROTOCOL", "SASL_PLAINTEXT")
//                .withEnv("KAFKA_SASL_ENABLED_MECHANISM", "PLAIN")
//                .withEnv("KAFKA_LISTENERS", "PLAINTEXT://:9093,SASL_PLAINTEXT://:9092")
//                .withEnv("KAFKA_ADVERTISED_LISTENERS", String.format("PLAINTEXT://%s:9093,SASL_PLAINTEXT://%s:9092", hostname, hostname))
//                .withEnv("KAFKA_ALLOW_EVERYONE_IF_NO_ACL_FOUND", "true")

                .withNetworkAliases(hostname)
                .withExtraHost(hostname, "127.0.0.1")

                .withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT")
                .withEnv("KAFKA_ADVERTISED_LISTENERS", String.format("PLAINTEXT://%s:9092,PLAINTEXT_HOST://localhost:%s", hostname, port))

//                .withEnv("KAFKA_ADVERTISED_LISTENERS", String.format("PLAINTEXT://%s:9092", hostname))
                .withEnv("KAFKA_BROKER_ID", "0")
                .withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1")
                .withEnv("KAFKA_ZOOKEEPER_CONNECT", String.format("zookeeper:%s/%s", zkPort, basePath))
                .waitingFor(Wait.forListeningPort());

//        sasl.mechanism=PLAIN
//        security.protocol=SASL_PLAINTEXT
//        sasl.enabled.mechanisms=PLAIN
//        listeners=PLAINTEXT://:9093,SASL_PLAINTEXT://:9092
//        advertised.listeners=PLAINTEXT://localhost:9093,SASL_PLAINTEXT://localhost:9092
//        allow.everyone.if.no.acl.found=true

//                .waitingFor(Wait.defaultWaitStrategy());
    }

    @Override
    public void start() throws Exception {
        logger.info("Starting in-memory Kafka");

        createZkBasePathIfNeeded();
        kafkaContainer.start();

        running = true;
    }

    @Override
    public void stop() throws Exception {
        if (running) {
            logger.info("Stopping in-memory Kafka");
            kafkaContainer.stop();
            running = false;
        } else {
            logger.info("In-memory Kafka is not running");
        }
    }

    @Override
    public KafkaServerStartable instance() {
        return null;
    }

    private CuratorFramework startZookeeperClient(int zkPort) throws InterruptedException {
        CuratorFramework zookeeperClient = CuratorFrameworkFactory.builder()
                .connectString(String.format("localhost:%s", zkPort))
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .build();
        zookeeperClient.start();
        zookeeperClient.blockUntilConnected();
        return zookeeperClient;
    }

    private void createZkBasePathIfNeeded() throws Exception {
        CuratorFramework curator = startZookeeperClient(zkPort);
        if (curator.checkExists().forPath("/" + basePath) == null) {
            curator.create().forPath("/" + basePath);
        }
        curator.close();
    }

}
