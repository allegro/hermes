package pl.allegro.tech.hermes.integrationtests.setup;

import org.springframework.boot.builder.SpringApplicationBuilder;
import pl.allegro.tech.hermes.frontend.HermesFrontend;
import pl.allegro.tech.hermes.test.helper.containers.KafkaContainerCluster;
import pl.allegro.tech.hermes.test.helper.containers.ZookeeperContainer;
import pl.allegro.tech.hermes.test.helper.util.Ports;

class HermesFrontendTestApp implements HermesTestApp {

    private final ZookeeperContainer hermesZookeeper;
    private final KafkaContainerCluster kafka;
    private final int port = Ports.nextAvailable();
    private final SpringApplicationBuilder app = new SpringApplicationBuilder(HermesFrontend.class);

    HermesFrontendTestApp(ZookeeperContainer hermesZookeeper, KafkaContainerCluster kafka) {
        this.hermesZookeeper = hermesZookeeper;
        this.kafka = kafka;
    }

    @Override
    public void start() {
        app.run(
                "--frontend.server.port=" + port,
                "--frontend.kafka.clusters.[0].brokerList=" + kafka.getBootstrapServersForExternalClients(),
                "--frontend.zookeeper.clusters.[0].connectionString=" + hermesZookeeper.getConnectionString()
        );
    }

    @Override
    public void stop() {
        app.context().close();
    }

    int getPort() {
        return port;
    }
}
