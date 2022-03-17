package pl.allegro.tech.hermes.integration.env;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.apache.curator.framework.CuratorFramework;
import org.assertj.core.api.Assertions;
import org.springframework.context.ConfigurableApplicationContext;
import pl.allegro.tech.hermes.integration.helper.graphite.GraphiteMockServer;
import pl.allegro.tech.hermes.test.helper.environment.Starter;
import pl.allegro.tech.hermes.test.helper.environment.WireMockStarter;

import java.util.Map;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public final class SharedServices {

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
        WireMockServer wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        return wireMockServer;
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
