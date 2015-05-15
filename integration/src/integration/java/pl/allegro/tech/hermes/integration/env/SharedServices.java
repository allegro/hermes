package pl.allegro.tech.hermes.integration.env;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.apache.curator.framework.CuratorFramework;
import org.assertj.core.api.Assertions;
import pl.allegro.tech.hermes.consumers.HermesConsumers;
import pl.allegro.tech.hermes.integration.helper.graphite.GraphiteMockServer;
import pl.allegro.tech.hermes.test.helper.environment.KafkaStarter;
import pl.allegro.tech.hermes.test.helper.environment.Starter;
import pl.allegro.tech.hermes.test.helper.environment.WireMockStarter;

import java.util.Map;

public final class SharedServices {

    private static SharedServices services;

    private final Map<Class<?>, Starter<?>> starters;
    private final CuratorFramework zookeeper;
    private final CuratorFramework kafkaZookeeper;


    private SharedServices(Map<Class<?>, Starter<?>> starters, CuratorFramework zookeeper, CuratorFramework kafkaZookeeper) {
        this.starters = starters;
        this.zookeeper = zookeeper;
        this.kafkaZookeeper = kafkaZookeeper;
    }

    public static void initialize(Map<Class<?>, Starter<?>> starters, CuratorFramework zookeeper, CuratorFramework kafkaZookeeper) {
        services = new SharedServices(starters, zookeeper, kafkaZookeeper);
    }

    public static SharedServices services() {
        Assertions.assertThat(services).isNotNull();
        return services;
    }

    public WireMockServer serviceMock() {
        return ((WireMockStarter) starters.get(WireMockStarter.class)).instance();
    }

    public WireMockServer graphiteHttpMock() {
        return ((WireMockStarter) starters.get(GraphiteHttpMockStarter.class)).instance();
    }

    public WireMockServer oauthMock() {
        return ((WireMockStarter) starters.get(OAuthServerMockStarter.class)).instance();
    }

    public CuratorFramework zookeeper() {
        return zookeeper;
    }

    public CuratorFramework kafkaZookeeper() {
        return kafkaZookeeper;
    }

    public HermesConsumers consumers() {
        return ((ConsumersStarter) starters.get(ConsumersStarter.class)).instance();
    }

    public GraphiteMockServer graphiteMock() {
        return ((GraphiteMockStarter) starters.get(GraphiteMockStarter.class)).instance();
    }

    public KafkaStarter kafkaStarter() {
        return (KafkaStarter) starters.get(KafkaStarter.class);
    }

}
