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
import pl.allegro.tech.hermes.consumers.ConsumerConfigurationProperties;
import pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties;
import pl.allegro.tech.hermes.integration.setup.HermesManagementInstance;
import pl.allegro.tech.hermes.test.helper.containers.ConfluentSchemaRegistryContainer;
import pl.allegro.tech.hermes.test.helper.containers.GooglePubSubContainer;
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

import static pl.allegro.tech.hermes.consumers.ConsumerConfigurationProperties.GOOGLE_PUBSUB_TRANSPORT_CHANNEL_PROVIDER_ADDRESS;
import static pl.allegro.tech.hermes.infrastructure.dc.DefaultDatacenterNameProvider.DEFAULT_DC_NAME;

@Listeners({RetryListener.class})
public class HermesIntegrationEnvironment implements EnvironmentAware {

    private static final Logger logger = LoggerFactory.getLogger(HermesIntegrationEnvironment.class);

    private static final Map<Class<?>, Starter<?>> STARTERS = new LinkedHashMap<>();

    public static final String DC1 = DEFAULT_DC_NAME;
    public static final String DC2 = "dc2";
    private static final int NUMBER_OF_BROKERS_PER_CLUSTER = 3;

    public static final KafkaContainerCluster kafkaClusterOne = new KafkaContainerCluster(NUMBER_OF_BROKERS_PER_CLUSTER);
    public static final KafkaContainerCluster kafkaClusterTwo = new KafkaContainerCluster(NUMBER_OF_BROKERS_PER_CLUSTER);
    public static final ZookeeperContainer hermesZookeeperOne = new ZookeeperContainer("ZookeeperContainerOne");
    public static final ZookeeperContainer hermesZookeeperTwo = new ZookeeperContainer();
    public static final GooglePubSubContainer googlePubSubEmulator = new GooglePubSubContainer();
    public static final ConfluentSchemaRegistryContainer schemaRegistry = new ConfluentSchemaRegistryContainer()
            .withKafkaCluster(kafkaClusterOne);
    public static HermesManagementInstance managementStarter;
    private static CuratorFramework zookeeper;

    static {
        // set properties before any other test initialization
        System.setProperty("zookeeper.sasl.client", "false");
        System.setProperty(
                "java.security.auth.login.config",
                HermesIntegrationEnvironment.class.getClassLoader().getResource("kafka_server_jaas.conf").getPath()
        );
    }

    static {
        STARTERS.put(GraphiteMockStarter.class, new GraphiteMockStarter(GRAPHITE_SERVER_PORT));
        STARTERS.put(WireMockStarter.class, new WireMockStarter(HTTP_ENDPOINT_PORT));
        STARTERS.put(GraphiteHttpMockStarter.class, new GraphiteHttpMockStarter());
        STARTERS.put(OAuthServerMockStarter.class, new OAuthServerMockStarter());
        STARTERS.put(AuditEventMockStarter.class, new AuditEventMockStarter());
        STARTERS.put(JmsStarter.class, new JmsStarter());
    }

    @BeforeSuite
    public void prepareEnvironment(ITestContext context) {
        try {
            Stream.of(kafkaClusterOne, kafkaClusterTwo, hermesZookeeperOne, hermesZookeeperTwo, googlePubSubEmulator)
                    .parallel()
                    .forEach(Startable::start);

            schemaRegistry.start();

            for (Starter<?> starter : STARTERS.values()) {
                starter.start();
            }

            managementStarter = HermesManagementInstance.starter()
                    .port(MANAGEMENT_PORT)
                    .addKafkaCluster(DC1, kafkaClusterOne.getBootstrapServersForExternalClients())
                    .addZookeeperCluster(DC1, hermesZookeeperOne.getConnectionString())
                    .schemaRegistry(schemaRegistry.getUrl())
                    .replicationFactor(kafkaClusterOne.getAllBrokers().size())
                    .uncleanLeaderElectionEnabled(false)
                    .start();
            // Since we don't start a management instance for DC2 we need to create the root path manually.
            initializeRootPathInZookeeperTwo();

            zookeeper = startZookeeperClient(hermesZookeeperOne.getConnectionString());

            ConsumersStarter consumersStarter = new ConsumersStarter();
            consumersStarter.overrideProperty(ConsumerConfigurationProperties.KAFKA_AUTHORIZATION_ENABLED, false);
            consumersStarter.overrideProperty(ConsumerConfigurationProperties.KAFKA_CLUSTER_NAME, PRIMARY_KAFKA_CLUSTER_NAME);
            consumersStarter.overrideProperty(
                    ConsumerConfigurationProperties.KAFKA_BROKER_LIST, kafkaClusterOne.getBootstrapServersForExternalClients()
            );
            consumersStarter.overrideProperty(
                    ConsumerConfigurationProperties.ZOOKEEPER_CONNECTION_STRING, hermesZookeeperOne.getConnectionString()
            );
            consumersStarter.overrideProperty(ConsumerConfigurationProperties.SCHEMA_REPOSITORY_SERVER_URL, schemaRegistry.getUrl());
            consumersStarter.overrideProperty(GOOGLE_PUBSUB_TRANSPORT_CHANNEL_PROVIDER_ADDRESS, googlePubSubEmulator.getEmulatorEndpoint());
            consumersStarter.start();

            FrontendStarter frontendStarter = FrontendStarter.withCommonIntegrationTestConfig(FRONTEND_PORT);
            frontendStarter.overrideProperty(
                    FrontendConfigurationProperties.KAFKA_BROKER_LIST, kafkaClusterOne.getBootstrapServersForExternalClients()
            );
            frontendStarter.overrideProperty(
                    FrontendConfigurationProperties.ZOOKEEPER_CONNECTION_STRING, hermesZookeeperOne.getConnectionString()
            );
            frontendStarter.overrideProperty(FrontendConfigurationProperties.SCHEMA_REPOSITORY_SERVER_URL, schemaRegistry.getUrl());
            frontendStarter.overrideProperty(FrontendConfigurationProperties.METRICS_GRAPHITE_REPORTER_ENABLED, true);
            frontendStarter.overrideProperty(FrontendConfigurationProperties.GRAPHITE_PORT, 18023);
            frontendStarter.start();

            for (ITestNGMethod method : context.getAllTestMethods()) {
                method.setRetryAnalyzerClass(Retry.class);
            }

            SharedServices.initialize(STARTERS, zookeeper);
            logger.info("Environment was prepared");
        } catch (Exception e) {
            throw new RuntimeException("Exception during environment preparation", e);
        }
    }

    private CuratorFramework startZookeeperClient(String connectString) {
        final CuratorFramework zookeeperClient = CuratorFrameworkFactory.builder()
                .connectString(connectString)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .build();
        zookeeperClient.start();
        return zookeeperClient;
    }

    private void initializeRootPathInZookeeperTwo() throws Exception {
        try (CuratorFramework curatorFramework = startZookeeperClient(hermesZookeeperTwo.getConnectionString())) {
            curatorFramework.create().creatingParentsIfNeeded().forPath("/hermes/groups");
        }
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
