package pl.allegro.tech.hermes.integration;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.frontend.HermesFrontend;
import pl.allegro.tech.hermes.frontend.server.HermesServer;
import pl.allegro.tech.hermes.test.helper.config.MutableConfigFactory;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesPublisher;
import pl.allegro.tech.hermes.test.helper.util.Ports;

public abstract class AbstractFrontendShutdownTest extends IntegrationTest {

    public static final int FRONTEND_PORT = Ports.nextAvailable();
    public static final String FRONTEND_URL = "http://127.0.0.1:" + FRONTEND_PORT;

    protected HermesPublisher publisher;
    protected HermesServer hermesServer;

    private HermesFrontend hermesFrontend;

    @BeforeClass
    public void setup() throws Exception {
        ConfigFactory configFactory = new MutableConfigFactory()
                .overrideProperty(Configs.FRONTEND_PORT, FRONTEND_PORT)
                .overrideProperty(Configs.FRONTEND_SSL_ENABLED, false)
                .overrideProperty(Configs.FRONTEND_GRACEFUL_SHUTDOWN_ENABLED, false);

        hermesFrontend = HermesFrontend.frontend()
                .withBinding(configFactory, ConfigFactory.class)
                .withDisabledGlobalShutdownHook()
                .withDisabledFlushLogsShutdownHook()
                .build();

        hermesFrontend.start();

        hermesServer = hermesFrontend.getService(HermesServer.class);

        publisher = new HermesPublisher(FRONTEND_URL);
    }

    @AfterClass
    public void tearDown() throws InterruptedException {
        hermesFrontend.stop();
    }

}
