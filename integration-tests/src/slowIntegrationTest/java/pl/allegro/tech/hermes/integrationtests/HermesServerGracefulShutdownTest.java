package pl.allegro.tech.hermes.integrationtests;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.lifecycle.Startable;
import pl.allegro.tech.hermes.frontend.server.HermesServer;
import pl.allegro.tech.hermes.integrationtests.client.FrontendTestClient;
import pl.allegro.tech.hermes.integrationtests.setup.HermesFrontendTestApp;
import pl.allegro.tech.hermes.test.helper.containers.ConfluentSchemaRegistryContainer;
import pl.allegro.tech.hermes.test.helper.containers.KafkaContainerCluster;
import pl.allegro.tech.hermes.test.helper.containers.ZookeeperContainer;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.awaitility.Awaitility.waitAtMost;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_GRACEFUL_SHUTDOWN_ENABLED;

public class HermesServerGracefulShutdownTest  {

    private static final ZookeeperContainer hermesZookeeper = new ZookeeperContainer("HermesZookeeper");
    private static final KafkaContainerCluster kafka = new KafkaContainerCluster(1);
    private static final ConfluentSchemaRegistryContainer schemaRegistry = new ConfluentSchemaRegistryContainer()
            .withKafkaCluster(kafka);

    private HermesFrontendTestApp frontend;
    private HermesServer hermesServer;
    FrontendTestClient frontendClient;

    @BeforeAll
    public static void beforeAll() {
        Stream.of(hermesZookeeper, kafka).parallel().forEach(Startable::start);
        schemaRegistry.start();
    }

    @AfterAll
    public static void afterAll() {
        Stream.of(hermesZookeeper, kafka, schemaRegistry)
                .parallel()
                .forEach(Startable::stop);
    }

    @BeforeEach
    public void beforEach() {
        frontend = new HermesFrontendTestApp(hermesZookeeper, kafka, schemaRegistry);
        frontend.withProperty(FRONTEND_GRACEFUL_SHUTDOWN_ENABLED, false);
        frontend.start();
        hermesServer = frontend.getBean(HermesServer.class);
        frontendClient = new FrontendTestClient(frontend.getPort());
    }

    @AfterEach
    public void afterEach() {
        frontend.stop();
    }

    @Test
    public void shouldShutdownGracefully() throws Throwable {
        //given
        hermesServer.prepareForGracefulShutdown();

        //when
        WebTestClient.ResponseSpec response = frontendClient.publish("topic", TestMessage.of("hello", "world").body());

        //then
        response.expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    public void shouldReturnCorrectHealthStatus() throws InterruptedException {
        // when
        hermesServer.prepareForGracefulShutdown();

        // then
        waitAtMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> frontendClient.getStatusPing().expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

}
