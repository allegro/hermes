package pl.allegro.tech.hermes.integrationtests.setup;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import pl.allegro.tech.hermes.frontend.HermesFrontend;
import pl.allegro.tech.hermes.frontend.server.HermesServer;
import pl.allegro.tech.hermes.test.helper.containers.KafkaContainerCluster;
import pl.allegro.tech.hermes.test.helper.containers.ZookeeperContainer;

import java.time.Duration;

public class HermesFrontendTestApp implements HermesTestApp {

    private final ZookeeperContainer hermesZookeeper;
    private final KafkaContainerCluster kafka;
    private final SpringApplicationBuilder app = new SpringApplicationBuilder(HermesFrontend.class)
            .web(WebApplicationType.NONE);

    private int port = -1;
    private boolean kafkaCheckEnabled = false;
    private Duration metadataMaxAge = Duration.ofMinutes(5);
    private Duration readinessCheckInterval = Duration.ofSeconds(1);

    public HermesFrontendTestApp(ZookeeperContainer hermesZookeeper, KafkaContainerCluster kafka) {
        this.hermesZookeeper = hermesZookeeper;
        this.kafka = kafka;
    }

    @Override
    public HermesTestApp start() {
        app.run(
                "--frontend.server.port=0",
                "--frontend.kafka.namespace=itTest",
                "--frontend.kafka.clusters.[0].brokerList=" + kafka.getBootstrapServersForExternalClients(),
                "--frontend.zookeeper.clusters.[0].connectionString=" + hermesZookeeper.getConnectionString(),
                "--frontend.readiness.check.kafkaCheckEnabled=" + kafkaCheckEnabled,
                "--frontend.readiness.check.enabled=true",
                "--frontend.header.propagation.enabled=true",
                "--frontend.header.propagation.allowFilter=" + "Trace-Id, Span-Id, Parent-Span-Id, Trace-Sampled, Trace-Reported",
                "--frontend.kafka.producer.metadataMaxAge=" + metadataMaxAge,
                "--frontend.readiness.check.interval=" + readinessCheckInterval
        );
        port = app.context().getBean(HermesServer.class).getPort();
        return this;
    }

    @Override
    public void stop() {
        app.context().close();
    }

    @Override
    public int getPort() {
        if (port == -1) {
            throw new IllegalStateException("hermes-frontend port hasn't been initialized");
        }
        return port;
    }

    public HermesFrontendTestApp metadataMaxAgeInSeconds(int value) {
        metadataMaxAge = Duration.ofSeconds(value);
        return this;
    }

    public HermesFrontendTestApp readinessCheckIntervalInSeconds(int value) {
        readinessCheckInterval = Duration.ofSeconds(value);
        return this;
    }

    public HermesFrontendTestApp kafkaCheckEnabled() {
        kafkaCheckEnabled = true;
        return this;
    }

    public HermesFrontendTestApp kafkaCheckDisabled() {
        kafkaCheckEnabled = false;
        return this;
    }
}
