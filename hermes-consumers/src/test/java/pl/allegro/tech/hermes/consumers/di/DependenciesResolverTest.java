package pl.allegro.tech.hermes.consumers.di;

import org.apache.curator.test.TestingServer;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.di.CommonBinder;
import pl.allegro.tech.hermes.consumers.consumer.sender.MessageSenderProviders;

import java.io.IOException;

import static org.mockito.Mockito.doReturn;

@RunWith(MockitoJUnitRunner.class)
public class DependenciesResolverTest {

    private static final String USER_DIR = "user.dir";
    private static TestingServer testingServer;

    @Spy
    private ConfigFactory configFactory;

    @BeforeClass
    public static void beforeClass() throws Exception {
        if (System.getProperty(USER_DIR).endsWith("consumers")) {
            System.setProperty(USER_DIR, System.getProperty(USER_DIR) + "/..");
        }
        testingServer = new TestingServer();
    }

    @AfterClass
    public static void afterClass() throws IOException {
        testingServer.stop();
    }

    @Before
    public void setUp() {
        doReturn(testingServer.getConnectString())
                .when(configFactory).getStringProperty(Configs.ZOOKEEPER_CONNECT_STRING);
        doReturn(testingServer.getConnectString())
                .when(configFactory).getStringProperty(Configs.KAFKA_ZOOKEEPER_CONNECT_STRING);
    }

    @Test
    public void shouldGetAllServicesWithoutAnyExceptions() {
        ServiceLocator serviceLocator = ServiceLocatorUtilities.bind("serviceLocatorTestName1",
                new CommonBinder(), new ConsumersBinder(), new TestBinder(configFactory)
        );

        serviceLocator.getAllServices(d -> true);
    }

    private static final class TestBinder extends AbstractBinder {

        private final ConfigFactory configFactory;

        private TestBinder(ConfigFactory configFactory) {
            this.configFactory = configFactory;
        }

        @Override
        protected void configure() {
            bind(this.configFactory).to(ConfigFactory.class).ranked(10);
            bind(MessageSenderProviders.class).to(MessageSenderProviders.class);
        }
    }
}
