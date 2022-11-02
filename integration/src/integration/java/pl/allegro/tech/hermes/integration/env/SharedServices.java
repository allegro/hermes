package pl.allegro.tech.hermes.integration.env;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.apache.curator.framework.CuratorFramework;
import org.assertj.core.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import pl.allegro.tech.hermes.integration.helper.graphite.GraphiteMockServer;
import pl.allegro.tech.hermes.test.helper.environment.Starter;
import pl.allegro.tech.hermes.test.helper.environment.WireMockStarter;

import java.util.Map;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public final class SharedServices {

    Logger logger = LoggerFactory.getLogger(SharedServices.class);

    private static SharedServices services;

    private final Map<Class<?>, Starter<?>> starters;
    private final CuratorFramework zookeeper;


    private SharedServices(Map<Class<?>, Starter<?>> starters, CuratorFramework zookeeper) {
        this.starters = starters;
        this.zookeeper = zookeeper;
    }

    public static void initialize(Map<Class<?>, Starter<?>> starters, CuratorFramework zookeeper) {
        services = new SharedServices(starters, zookeeper);
    }

    public static SharedServices services() {
        Assertions.assertThat(services).isNotNull();
        return services;
    }

    public WireMockServer serviceMock() {
        WireMockStarter wireMockStarter = new WireMockStarter(wireMockConfig().dynamicPort().portNumber());
        try {
            wireMockStarter.start();
        } catch (Exception exception) {
            logger.error("Error while starting wiremock server");
        }
        return wireMockStarter.instance();
    }

    public WireMockServer graphiteHttpMock() {
        return ((WireMockStarter) starters.get(GraphiteHttpMockStarter.class)).instance();
    }

    public WireMockServer oauthMock() {
        return ((WireMockStarter) starters.get(OAuthServerMockStarter.class)).instance();
    }

    public WireMockServer auditEventMock() {
        return ((WireMockStarter) starters.get(AuditEventMockStarter.class)).instance();
    }

    public CuratorFramework zookeeper() {
        return zookeeper;
    }

    public ConfigurableApplicationContext consumers() {
        return ((ConsumersStarter) starters.get(ConsumersStarter.class)).instance();
    }

    public GraphiteMockServer graphiteMock() {
        return ((GraphiteMockStarter) starters.get(GraphiteMockStarter.class)).instance();
    }
}
