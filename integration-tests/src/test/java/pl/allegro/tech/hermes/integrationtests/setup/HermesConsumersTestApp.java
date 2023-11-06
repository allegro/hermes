package pl.allegro.tech.hermes.integrationtests.setup;

import org.springframework.boot.builder.SpringApplicationBuilder;
import pl.allegro.tech.hermes.consumers.HermesConsumers;
import pl.allegro.tech.hermes.test.helper.containers.KafkaContainerCluster;
import pl.allegro.tech.hermes.test.helper.containers.ZookeeperContainer;
import pl.allegro.tech.hermes.test.helper.util.Ports;

class HermesConsumersTestApp implements HermesTestApp {

    private final ZookeeperContainer hermesZookeeper;
    private final KafkaContainerCluster kafka;
    private final int port = Ports.nextAvailable();
    private final SpringApplicationBuilder app = new SpringApplicationBuilder(HermesConsumers.class);

    HermesConsumersTestApp(ZookeeperContainer hermesZookeeper, KafkaContainerCluster kafka) {
        this.hermesZookeeper = hermesZookeeper;
        this.kafka = kafka;
    }

    @Override
    public void start() {
        app.run(
                "--server.port=" + port,
                "--consumer.kafka.clusters.[0].brokerList=" + kafka.getBootstrapServersForExternalClients(),
                "--consumer.zookeeper.clusters.[0].connectionString=" + hermesZookeeper.getConnectionString()
        );
    }

    @Override
    public void stop() {
        app.context().close();
    }
}
