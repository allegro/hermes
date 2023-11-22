package pl.allegro.tech.hermes.integrationtests.setup;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import pl.allegro.tech.hermes.consumers.HermesConsumers;
import pl.allegro.tech.hermes.consumers.server.ConsumerHttpServer;
import pl.allegro.tech.hermes.test.helper.containers.KafkaContainerCluster;
import pl.allegro.tech.hermes.test.helper.containers.ZookeeperContainer;

import java.time.Duration;

class HermesConsumersTestApp implements HermesTestApp {

    private final ZookeeperContainer hermesZookeeper;
    private final KafkaContainerCluster kafka;

    private int port = -1;

    private final SpringApplicationBuilder app = new SpringApplicationBuilder(HermesConsumers.class)
            .web(WebApplicationType.NONE);

    HermesConsumersTestApp(ZookeeperContainer hermesZookeeper, KafkaContainerCluster kafka) {
        this.hermesZookeeper = hermesZookeeper;
        this.kafka = kafka;
    }

    @Override
    public void start() {
        app.run(
                "--consumer.healthCheckPort=0",
                "--consumer.kafka.clusters.[0].brokerList=" + kafka.getBootstrapServersForExternalClients(),
                "--consumer.zookeeper.clusters.[0].connectionString=" + hermesZookeeper.getConnectionString(),
                "--consumer.backgroundSupervisor.interval=" + Duration.ofMillis(100),
                "--consumer.workload.rebalanceInterval=" + Duration.ofSeconds(1),
                "--consumer.commit.offset.period=" + Duration.ofSeconds(1)
        );
        port = app.context().getBean(ConsumerHttpServer.class).getPort();
    }

    @Override
    public void stop() {
        app.context().close();
    }

    int getPort() {
        if (port == -1) {
            throw new IllegalStateException("hermes-consumers port hasn't been initialized");
        }
        return port;
    }
}
