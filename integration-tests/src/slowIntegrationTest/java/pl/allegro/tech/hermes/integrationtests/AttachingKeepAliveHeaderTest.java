package pl.allegro.tech.hermes.integrationtests;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.lifecycle.Startable;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integrationtests.client.FrontendTestClient;
import pl.allegro.tech.hermes.integrationtests.setup.HermesFrontendTestApp;
import pl.allegro.tech.hermes.integrationtests.setup.HermesInitHelper;
import pl.allegro.tech.hermes.integrationtests.setup.HermesManagementTestApp;
import pl.allegro.tech.hermes.test.helper.containers.ConfluentSchemaRegistryContainer;
import pl.allegro.tech.hermes.test.helper.containers.KafkaContainerCluster;
import pl.allegro.tech.hermes.test.helper.containers.ZookeeperContainer;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import java.util.function.Consumer;
import java.util.stream.Stream;

import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_KEEP_ALIVE_HEADER_ENABLED;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.FRONTEND_KEEP_ALIVE_HEADER_TIMEOUT;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

public class AttachingKeepAliveHeaderTest {

    private static final ZookeeperContainer hermesZookeeper = new ZookeeperContainer("HermesZookeeper");
    private static final KafkaContainerCluster kafka = new KafkaContainerCluster(1);
    private static final ConfluentSchemaRegistryContainer schemaRegistry = new ConfluentSchemaRegistryContainer()
            .withKafkaCluster(kafka);
    private static final HermesManagementTestApp management = new HermesManagementTestApp(hermesZookeeper, kafka, schemaRegistry);
    private static HermesInitHelper initHelper;

    private static final String MESSAGE = TestMessage.of("hello", "world").body();

    @BeforeAll
    public static void setup() {
        Stream.of(hermesZookeeper, kafka).parallel().forEach(Startable::start);
        schemaRegistry.start();
        management.start();
        initHelper = new HermesInitHelper(management.getPort());
    }

    @AfterAll
    public static void clean() {
        management.stop();
        Stream.of(hermesZookeeper, kafka, schemaRegistry)
                .parallel()
                .forEach(Startable::stop);
    }

    @Test
    public void shouldAttachKeepAliveHeaderWhenEnabled() {
        //given
        HermesFrontendTestApp frontend = startFrontend(f -> {
            f.withProperty(FRONTEND_KEEP_ALIVE_HEADER_ENABLED, true);
            f.withProperty(FRONTEND_KEEP_ALIVE_HEADER_TIMEOUT, "2s");
        });

        Topic topic = initHelper.createTopic(topicWithRandomName().build());

        FrontendTestClient publisher = new FrontendTestClient(frontend.getPort());

        try {
            //when
            WebTestClient.ResponseSpec response = publisher.publish(topic.getQualifiedName(), MESSAGE);

            //then
            response.expectHeader().valueEquals("Keep-Alive", "timeout=2");
        } finally {
            frontend.stop();
        }
    }

    @Test
    public void shouldNotAttachKeepAliveHeaderWhenDisabled() {
        //given
        HermesFrontendTestApp frontend = startFrontend(f -> {
            f.withProperty(FRONTEND_KEEP_ALIVE_HEADER_ENABLED, false);
        });

        Topic topic = initHelper.createTopic(topicWithRandomName().build());

        FrontendTestClient publisher = new FrontendTestClient(frontend.getPort());

        try {
            //when
            WebTestClient.ResponseSpec response = publisher.publish(topic.getQualifiedName(), MESSAGE);

            //then
            response.expectHeader().doesNotExist("Keep-Alive");
        } finally {
            frontend.stop();
        }
    }

    private HermesFrontendTestApp startFrontend(Consumer<HermesFrontendTestApp> frontendConfigUpdater) {
        HermesFrontendTestApp frontend = new HermesFrontendTestApp(hermesZookeeper, kafka, schemaRegistry);
        frontendConfigUpdater.accept(frontend);
        frontend.start();
        return frontend;
    }
}
