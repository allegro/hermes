package pl.allegro.tech.hermes.integration;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties;
import pl.allegro.tech.hermes.integration.env.FrontendStarter;
import pl.allegro.tech.hermes.test.helper.endpoint.HermesPublisher;
import pl.allegro.tech.hermes.test.helper.message.TestMessage;
import pl.allegro.tech.hermes.test.helper.util.Ports;

import java.util.Arrays;
import java.util.Optional;

import static jakarta.ws.rs.core.Response.Status.CREATED;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.randomTopic;

public class BrokerLatencyReportingTest extends IntegrationTest {

    private static final int FRONTEND_PORT = Ports.nextAvailable();
    private static final String FRONTEND_URL = "http://127.0.0.1:" + FRONTEND_PORT;
    private HermesPublisher publisher;
    private FrontendStarter frontendStarter;
    private final Client client = ClientBuilder.newClient();


    @BeforeClass
    public void setup() throws Exception {
        frontendStarter = FrontendStarter.withCommonIntegrationTestConfig(FRONTEND_PORT);
        frontendStarter.overrideProperty(
                FrontendConfigurationProperties.KAFKA_BROKER_LIST, kafkaClusterOne.getBootstrapServersForExternalClients()
        );
        frontendStarter.overrideProperty(
                FrontendConfigurationProperties.ZOOKEEPER_CONNECTION_STRING, hermesZookeeperOne.getConnectionString()
        );
        frontendStarter.overrideProperty(FrontendConfigurationProperties.SCHEMA_REPOSITORY_SERVER_URL, schemaRegistry.getUrl());
        frontendStarter.overrideProperty(FrontendConfigurationProperties.METRICS_GRAPHITE_REPORTER_ENABLED, true);
        frontendStarter.overrideProperty(FrontendConfigurationProperties.GRAPHITE_PORT, 18023);
        frontendStarter.overrideProperty(FrontendConfigurationProperties.BROKER_LATENCY_REPORTER_ENABLED, true);
        frontendStarter.start();

        publisher = new HermesPublisher(FRONTEND_URL);
    }

    @AfterClass
    public void tearDown() throws Exception {
        frontendStarter.stop();
    }

    @Test
    public void shouldReportBrokerLatencyMetrics() {
        // given
        Topic topic = operations.buildTopic(randomTopic("brokerLatency", "topic").build());

        TestMessage message = TestMessage.of("hello", "world");

        // when
        Response response = publisher.publish(topic.getQualifiedName(), message.body());

        // then
        assertThat(response).hasStatus(CREATED);
        wait.until(() -> {
            Double metricValue = getMetricValue("hermes_frontend_broker_latency_seconds_count{ack=\"LEADER\",broker=\"localhost\",")
                    .orElse(0.0);
            assertThat(metricValue).isGreaterThan(0.0d);
        }, 5);
    }

    private Optional<Double> getMetricValue(String metricPrefix) {
        String metricsResponse = client.target(FRONTEND_URL + "/status/prometheus").request().get(String.class);
        return Arrays.stream(metricsResponse.split("\n"))
                .filter(metricName -> metricName.startsWith(metricPrefix))
                .findFirst()
                .map(line -> line.split(" ")[1]) // metrics have format "<metric_name> <metric_value>"
                .map(Double::valueOf);
    }
}
