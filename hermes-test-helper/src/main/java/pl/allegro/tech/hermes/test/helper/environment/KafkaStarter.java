package pl.allegro.tech.hermes.test.helper.environment;

import com.jayway.awaitility.Duration;
import kafka.server.KafkaConfig;
import kafka.server.KafkaServerStartable;
import org.apache.commons.io.FileUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Properties;

import static com.jayway.awaitility.Awaitility.await;

public class KafkaStarter implements Starter<KafkaServerStartable> {

    private static final Logger logger = LoggerFactory.getLogger(KafkaStarter.class);

    private final KafkaConfig kafkaConfig;
    private KafkaServerStartable kafka;
    private boolean running;

    public KafkaStarter() {
        kafkaConfig = new KafkaConfig(loadProperties("/kafkalocal.properties"));
        cleanLogs();
    }

    public KafkaStarter(Properties kafkaProperties) {
        kafkaConfig = new KafkaConfig(kafkaProperties);
        cleanLogs();
    }

    public KafkaStarter(String configPath) {
        kafkaConfig = new KafkaConfig(loadProperties(configPath));
        cleanLogs();
    }

    @Override
    public void start() throws Exception {
        logger.info("Starting in-memory Kafka");

        createZkBasePathIfNeeded(kafkaConfig.zkConnect());

        kafka = new KafkaServerStartable(kafkaConfig);
        kafka.startup();

        waitForStartup(kafkaConfig.port());
        running = true;
    }

    private Properties loadProperties(String resourcesPath) {
        Properties properties = new Properties();
        try {
            logger.info("Loading kafka properties file: {}", resourcesPath);
            properties.load(this.getClass().getResourceAsStream(resourcesPath));
        } catch (IOException e) {
            throw new IllegalStateException("Error while loading kafka properties", e);
        }
        return properties;
    }

    private void waitForStartup(int port) {
        final InetSocketAddress inetSocketAddress = new InetSocketAddress("localhost", port);

        await().atMost(Duration.FIVE_SECONDS).until(() -> {
            try {
                Socket socket = new Socket();
                socket.connect(inetSocketAddress, 1000);
                return socket.isConnected();
            } catch (IOException e) {
                return false;
            }
        });
    }

    @Override
    public void stop() throws Exception {
        if (running) {
            System.clearProperty("java.security.auth.login.config");

            logger.info("Stopping in-memory Kafka");
            kafka.shutdown();
            kafka.awaitShutdown();
            running = false;
        } else {
            logger.info("In-memory Kafka is not running");
        }
    }

    @Override
    public KafkaServerStartable instance() {
        return kafka;
    }

    private void cleanLogs() {
        try {
            FileUtils.deleteDirectory(new File(kafkaConfig.logDirs().head()));
        } catch (IOException e) {
            logger.info("Error while removing kafka logs", e);
            throw new IllegalStateException(e);
        }
    }

    private CuratorFramework startZookeeperClient(String connectString) throws InterruptedException {
        CuratorFramework zookeeperClient = CuratorFrameworkFactory.builder()
                .connectString(connectString)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .build();
        zookeeperClient.start();
        zookeeperClient.blockUntilConnected();
        return zookeeperClient;
    }

    private void createZkBasePathIfNeeded(String connectString) throws Exception {
        String[] zkConnectStringSplitted = connectString.split("/", 2);

        if (zkConnectStringSplitted.length > 1) {
            CuratorFramework curator = startZookeeperClient(zkConnectStringSplitted[0]);
            if (curator.checkExists().forPath("/" + zkConnectStringSplitted[1]) == null) {
                curator.create().forPath("/" + zkConnectStringSplitted[1]);
            }
            curator.close();
        }
    }

}
