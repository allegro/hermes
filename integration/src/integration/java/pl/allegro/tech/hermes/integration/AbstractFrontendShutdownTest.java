package pl.allegro.tech.hermes.integration;

import com.mongodb.DB;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.di.CommonBinder;
import pl.allegro.tech.hermes.frontend.di.FrontendBinder;
import pl.allegro.tech.hermes.frontend.listeners.BrokerListeners;
import pl.allegro.tech.hermes.frontend.server.HermesServer;
import pl.allegro.tech.hermes.integration.env.FongoFactory;
import pl.allegro.tech.hermes.integration.env.MutableConfigFactory;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesPublisher;

import javax.inject.Singleton;

public abstract class AbstractFrontendShutdownTest extends IntegrationTest {

    public static final int FRONTEND_PORT = 8991;
    public static final String FRONTEND_URL = "http://0.0.0.0:" + FRONTEND_PORT;
    public static final String METRICS_REGISTRY_NAME = "integration-test-metric-registry";

    HermesPublisher publisher;
    HermesServer hermesServer;
    ServiceLocator serviceLocator;

    @BeforeClass
    public void setup() throws Exception {
        serviceLocator = ServiceLocatorUtilities.bind("secondary",
                new CommonBinder(), new FrontendBinder(), testBinder(), new AbstractBinder() {
                    @Override
                    protected void configure() {
                        bind(new BrokerListeners()).to(BrokerListeners.class);
                    }
                });
        hermesServer = serviceLocator.getService(HermesServer.class);
        hermesServer.start();

        publisher = new HermesPublisher(FRONTEND_URL);
    }

    @AfterClass
    public void tearDown() throws InterruptedException {
        hermesServer.shutdown();
        serviceLocator.shutdown();
    }

    private AbstractBinder testBinder() {
        return new AbstractBinder() {
            @Override
            protected void configure() {
                int rankHigherThanDefault = 10;
                ConfigFactory configFactory = new MutableConfigFactory()
                        .overrideProperty(Configs.FRONTEND_PORT, FRONTEND_PORT)
                        .overrideProperty(Configs.METRICS_REGISTRY_NAME, METRICS_REGISTRY_NAME);

                bind(configFactory).to(ConfigFactory.class).ranked(rankHigherThanDefault);
                bindFactory(new FongoFactory()).in(Singleton.class).to(DB.class).ranked(rankHigherThanDefault);
            }
        };
    }
}
