package pl.allegro.tech.hermes.integrationtests.setup;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import pl.allegro.tech.hermes.frontend.HermesFrontend;
import pl.allegro.tech.hermes.frontend.server.HermesServer;
import pl.allegro.tech.hermes.test.helper.containers.KafkaContainerCluster;
import pl.allegro.tech.hermes.test.helper.containers.ZookeeperContainer;

class HermesFrontendTestApp implements HermesTestApp {

    private final ZookeeperContainer hermesZookeeper;
    private final KafkaContainerCluster kafka;
    private int port = -1;
    private final SpringApplicationBuilder app = new SpringApplicationBuilder(HermesFrontend.class)
            .web(WebApplicationType.NONE);

    HermesFrontendTestApp(ZookeeperContainer hermesZookeeper, KafkaContainerCluster kafka) {
        this.hermesZookeeper = hermesZookeeper;
        this.kafka = kafka;
    }

    @Override
    public void start() {
        app.run(
                "--frontend.server.port=0",
                "--frontend.kafka.clusters.[0].brokerList=" + kafka.getBootstrapServersForExternalClients(),
                "--frontend.zookeeper.clusters.[0].connectionString=" + hermesZookeeper.getConnectionString()
        );
        port = app.context().getBean(HermesServer.class).getPort();
    }

    @Override
    public void stop() {
        app.context().close();
    }

    int getPort() {
        if (port == -1) {
            throw new IllegalStateException("hermes-frontend port hasn't been initialized");
        }
        return port;
    }
}
