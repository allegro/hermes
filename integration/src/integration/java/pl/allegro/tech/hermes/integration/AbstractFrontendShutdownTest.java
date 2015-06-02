package pl.allegro.tech.hermes.integration;

import com.mongodb.DB;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.frontend.HermesFrontend;
import pl.allegro.tech.hermes.frontend.server.HermesServer;
import pl.allegro.tech.hermes.integration.env.FongoFactory;
import pl.allegro.tech.hermes.integration.env.MutableConfigFactory;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesPublisher;

public abstract class AbstractFrontendShutdownTest extends IntegrationTest {

    public static final int FRONTEND_PORT = 8991;
    public static final String FRONTEND_URL = "http://0.0.0.0:" + FRONTEND_PORT;

    HermesPublisher publisher;
    HermesServer hermesServer;

    private HermesFrontend hermesFrontend;

    @BeforeClass
    public void setup() throws Exception {
        ConfigFactory configFactory = new MutableConfigFactory().overrideProperty(Configs.FRONTEND_PORT, FRONTEND_PORT);

        hermesFrontend = HermesFrontend.frontend()
                .withBinding(new FongoFactory().provide(), DB.class)
                .withBinding(configFactory, ConfigFactory.class)
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
