package pl.allegro.tech.hermes.consumers.di;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.junit.BeforeClass;
import org.junit.Test;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.config.Configs;
import pl.allegro.tech.hermes.common.di.CommonBinder;
import pl.allegro.tech.hermes.test.helper.config.MutableConfigFactory;
import pl.allegro.tech.hermes.test.helper.zookeeper.ZookeeperBaseTest;

public class DependenciesResolverTest extends ZookeeperBaseTest {

    private static final String USER_DIR = "user.dir";

    private static MutableConfigFactory configFactory;

    @BeforeClass
    public static void beforeClass() {
        configFactory = new MutableConfigFactory();
        configFactory.overrideProperty(Configs.ZOOKEEPER_CONNECT_STRING, zookeeperServer.getConnectString());

        if (System.getProperty(USER_DIR).endsWith("consumers")) {
            System.setProperty(USER_DIR, System.getProperty(USER_DIR) + "/..");
        }
    }

    @Test
    public void shouldGetAllServicesWithoutAnyExceptions() {
        ServiceLocator serviceLocator = ServiceLocatorUtilities.bind("serviceLocatorTestName1",
                new CommonBinder(), new ConsumersBinder(), new TestBinder(configFactory), new TrackersBinder()
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
        }
    }
}
