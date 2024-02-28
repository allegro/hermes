package pl.allegro.tech.hermes.integrationtests;

import com.jayway.awaitility.Duration;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
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

import java.util.Arrays;
import java.util.Optional;

import static com.jayway.awaitility.Awaitility.waitAtMost;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

public class BrokerLatencyReportingTest {

    private final Client client = ClientBuilder.newClient();

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
        waitAtMost(Duration.FIVE_SECONDS).until(() -> {
            Double metricValue = getMetricValue("hermes_frontend_broker_latency_seconds_count{ack=\"LEADER\",broker=\"localhost\",")
                    .orElse(0.0);
            assertThat(metricValue).isGreaterThan(0.0d);
        });
    }

    private Optional<Double> getMetricValue(String metricPrefix) {
        String metricsResponse = client.target("http://127.0.0.1:" + frontend.getPort() + "/status/prometheus").request().get(String.class);
        return Arrays.stream(metricsResponse.split("\n"))
                .filter(metricName -> metricName.startsWith(metricPrefix))
                .findFirst()
                .map(line -> line.split(" ")[1]) // metrics have format "<metric_name> <metric_value>"
                .map(Double::valueOf);
    }
}