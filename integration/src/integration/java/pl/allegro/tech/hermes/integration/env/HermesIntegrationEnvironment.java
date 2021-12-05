package pl.allegro.tech.hermes.integration.env;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.lifecycle.Startable;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.integration.setup.HermesManagementInstance;
import pl.allegro.tech.hermes.test.helper.containers.ConfluentSchemaRegistryContainer;
import pl.allegro.tech.hermes.test.helper.containers.KafkaContainerCluster;
import pl.allegro.tech.hermes.test.helper.containers.ZookeeperContainer;
import pl.allegro.tech.hermes.test.helper.environment.Starter;
import pl.allegro.tech.hermes.test.helper.environment.WireMockStarter;
import pl.allegro.tech.hermes.test.helper.retry.Retry;
import pl.allegro.tech.hermes.test.helper.retry.RetryListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import static pl.allegro.tech.hermes.management.infrastructure.dc.DefaultDatacenterNameProvider.DEFAULT_DC_NAME;

@Listeners({RetryListener.class})
public class HermesIntegrationEnvironment implements EnvironmentAware {

    private static final Logger logger = LoggerFactory.getLogger(HermesIntegrationEnvironment.class);

    private static final Map<Class<?>, Starter<?>> STARTERS = new LinkedHashMap<>();

    public static final String DC1 = DEFAULT_DC_NAME;
    public static final String DC2 = "dc2";
    private static final int NUMBER_OF_BROKERS_PER_CLUSTER = 3;

    public static final KafkaContainerCluster kafkaClusterOne = new KafkaContainerCluster(NUMBER_OF_BROKERS_PER_CLUSTER);
    public static final KafkaContainerCluster kafkaClusterTwo = new KafkaContainerCluster(NUMBER_OF_BROKERS_PER_CLUSTER);
    public static final ZookeeperContainer hermesZookeeperOne = new ZookeeperContainer();
    public static final ZookeeperContainer hermesZookeeperTwo = new ZookeeperContainer();
    public static ConfluentSchemaRegistryContainer schemaRegistry;
    public static HermesManagementInstance managementStarter;
    private static CuratorFramework zookeeper;

    static {
        // set properties before any other test initialization
        System.setProperty("zookeeper.sasl.client", "false");
        System.setProperty("java.security.auth.login.config", HermesIntegrationEnvironment.class.getClassLoader().getResource("kafka_server_jaas.conf").getPath());
    }

    static {
        STARTERS.put(GraphiteMockStarter.class, new GraphiteMockStarter(GRAPHITE_SERVER_PORT));
        STARTERS.put(WireMockStarter.class, new WireMockStarter(HTTP_ENDPOINT_PORT));
        STARTERS.put(GraphiteHttpMockStarter.class, new GraphiteHttpMockStarter());
        STARTERS.put(OAuthServerMockStarter.class, new OAuthServerMockStarter());
        STARTERS.put(JmsStarter.class, new JmsStarter());
    }

    @BeforeSuite
    public void prepareEnvironment(ITestContext context) throws Exception {
        try {
            Stream.of(kafkaClusterOne, kafkaClusterTwo, hermesZookeeperOne, hermesZookeeperTwo)
                    .parallel()
                    .forEach(Startable::start);

            schemaRegistry = new ConfluentSchemaRegistryContainer(kafkaClusterOne.getNetwork(), kafkaClusterOne.getBootstrapServersForInternalClients());
            schemaRegistry.start();

            ConsumersStarter consumersStarter = new ConsumersStarter();
            consumersStarter.overrideProperty(Configs.KAFKA_AUTHORIZATION_ENABLED, false);
            consumersStarter.overrideProperty(Configs.KAFKA_CLUSTER_NAME, PRIMARY_KAFKA_CLUSTER_NAME);
            consumersStarter.overrideProperty(Configs.KAFKA_BROKER_LIST, kafkaClusterOne.getBootstrapServers());
            consumersStarter.overrideProperty(Configs.ZOOKEEPER_CONNECT_STRING, hermesZookeeperOne.getConnectionString());
            consumersStarter.overrideProperty(Configs.SCHEMA_REPOSITORY_SERVER_URL, schemaRegistry.getUrl());
            STARTERS.put(ConsumersStarter.class, consumersStarter);

            FrontendStarter frontendStarter = new FrontendStarter(FRONTEND_PORT);
            frontendStarter.overrideProperty(Configs.KAFKA_AUTHORIZATION_ENABLED, false);
            frontendStarter.overrideProperty(Configs.KAFKA_BROKER_LIST, kafkaClusterOne.getBootstrapServers());
            frontendStarter.overrideProperty(Configs.ZOOKEEPER_CONNECT_STRING, hermesZookeeperOne.getConnectionString());
            frontendStarter.overrideProperty(Configs.SCHEMA_REPOSITORY_SERVER_URL, schemaRegistry.getUrl());
            STARTERS.put(FrontendStarter.class, frontendStarter);

            managementStarter = HermesManagementInstance.starter()
                    .port(MANAGEMENT_PORT)
                    .addKafkaCluster(DC1, kafkaClusterOne.getBootstrapServers())
                    .addZookeeperCluster(DC1, hermesZookeeperOne.getConnectionString())
                    .schemaRegistry(schemaRegistry.getUrl())
                    .replicationFactor(kafkaClusterOne.getAllBrokers().size())
                    .uncleanLeaderElectionEnabled(false)
                    .start();

            for (ITestNGMethod method : context.getAllTestMethods()) {
                method.setRetryAnalyzer(new Retry());
            }

            for (Starter<?> starter : STARTERS.values()) {
                starter.start();
            }

            zookeeper = startZookeeperClient();

            SharedServices.initialize(STARTERS, zookeeper);
            logger.info("Environment was prepared");
        } catch (Exception e) {
            logger.error("Exception during environment preparation", e);
        }
    }

    private CuratorFramework startZookeeperClient() throws InterruptedException {
        final CuratorFramework zookeeperClient = CuratorFrameworkFactory.builder()
                .connectString(hermesZookeeperOne.getConnectionString())
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .build();
        zookeeperClient.start();
        return zookeeperClient;
    }

    @AfterSuite(alwaysRun = true)
    public void cleanEnvironment() throws Exception {
        try {
            ArrayList<Starter<?>> reversedStarters = new ArrayList<>(STARTERS.values());
            Collections.reverse(reversedStarters);

            for (Starter<?> starter : reversedStarters) {
                starter.stop();
            }
            zookeeper.close();

            System.clearProperty("zookeeper.sasl.client");
            System.clearProperty("java.security.auth.login.config");

            logger.info("Environment cleaned");
        } catch (Exception e) {
            logger.error("Exception during environment cleaning", e);
        }
    }

    @Test
    public void shouldTriggerBeforeAndAfterMethods() {
    }
}
