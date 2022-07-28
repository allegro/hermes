package pl.allegro.tech.hermes.integration;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties;
import pl.allegro.tech.hermes.frontend.server.HermesServer;
import pl.allegro.tech.hermes.integration.env.FrontendStarter;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesPublisher;
import pl.allegro.tech.hermes.test.helper.util.Ports;

public abstract class AbstractFrontendShutdownTest extends IntegrationTest {

    public static final int FRONTEND_PORT = Ports.nextAvailable();
    public static final String FRONTEND_URL = "http://127.0.0.1:" + FRONTEND_PORT;

    protected HermesPublisher publisher;
    protected HermesServer hermesServer;
    protected FrontendStarter frontendStarter;

    @BeforeClass
    public void setup() throws Exception {
        frontendStarter = new FrontendStarter(FRONTEND_PORT);
        frontendStarter.overrideProperty(FrontendConfigurationProperties.FRONTEND_PORT, FRONTEND_PORT);
        frontendStarter.overrideProperty(FrontendConfigurationProperties.FRONTEND_SSL_ENABLED, false);
        frontendStarter.overrideProperty(FrontendConfigurationProperties.KAFKA_BROKER_LIST, kafkaClusterOne.getBootstrapServersForExternalClients());
        frontendStarter.overrideProperty(FrontendConfigurationProperties.ZOOKEEPER_CONNECTION_STRING, hermesZookeeperOne.getConnectionString());
        frontendStarter.overrideProperty(FrontendConfigurationProperties.SCHEMA_REPOSITORY_SERVER_URL, schemaRegistry.getUrl());
        frontendStarter.overrideProperty(FrontendConfigurationProperties.FRONTEND_GRACEFUL_SHUTDOWN_ENABLED, false);
        frontendStarter.start();

        hermesServer = frontendStarter.instance().getBean(HermesServer.class);
        publisher = new HermesPublisher(FRONTEND_URL);
    }

    @AfterClass
    public void tearDown() throws Exception {
        frontendStarter.stop();
    }
}
