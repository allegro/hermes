package pl.allegro.tech.hermes.integrationtests;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties;
import pl.allegro.tech.hermes.integrationtests.setup.HermesFrontendTestApp;
import pl.allegro.tech.hermes.integrationtests.setup.HermesManagementExtension;
import pl.allegro.tech.hermes.integrationtests.setup.InfrastructureExtension;
import pl.allegro.tech.hermes.test.helper.client.integration.FrontendTestClient;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;

import java.time.Duration;

import static org.awaitility.Awaitility.waitAtMost;
import static pl.allegro.tech.hermes.integrationtests.assertions.HermesAssertions.assertThatMetrics;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

public class BrokerLatencyReportingTest {

    @Order(0)
    @RegisterExtension
    public static InfrastructureExtension infra = new InfrastructureExtension();

    @Order(1)
    @RegisterExtension
    public static HermesManagementExtension management = new HermesManagementExtension(infra);

    private static HermesFrontendTestApp frontend;

    private static FrontendTestClient frontendTestClient;

    @BeforeAll
    public static void setup() {
        frontend = new HermesFrontendTestApp(infra.hermesZookeeper(), infra.kafka(), infra.schemaRegistry());
        frontend.withProperty(FrontendConfigurationProperties.BROKER_LATENCY_REPORTER_ENABLED, true);
        frontend.start();
        frontendTestClient = new FrontendTestClient(frontend.getPort());
    }

    @AfterAll
    public static void tearDown() {
        frontend.stop();
    }

    @Test
    public void shouldReportBrokerLatencyMetrics() {
        // given
        Topic topic = management.initHelper().createTopic(topicWithRandomName().build());

        TestMessage message = TestMessage.of("hello", "world");

        // when
        frontendTestClient.publishUntilSuccess(topic.getQualifiedName(), message.body());

        // then
        waitAtMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            frontendTestClient.getMetrics()
                    .expectStatus()
                    .isOk()
                    .expectBody(String.class)
                    .value((body) -> assertThatMetrics(body)
                            .contains("hermes_frontend_broker_latency_seconds_count")
                            .withLabels(
                                    "ack", "LEADER",
                                    "broker", "localhost"
                            )
                            .withValueGreaterThan(0.0d)
                    );
        });
    }
}