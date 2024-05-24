package pl.allegro.tech.hermes.integrationtests;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integrationtests.assertions.PrometheusMetricsAssertion;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;

import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.await;
import static pl.allegro.tech.hermes.integrationtests.assertions.HermesAssertions.assertThatMetrics;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

public class KafkaProducerMetricsTest {
    @RegisterExtension
    public static final HermesExtension hermes = new HermesExtension();

    @Test
    public void shouldRegisterSendMetrics() {
        // given
        Topic topic = hermes.initHelper().createTopic(topicWithRandomName().build());

        double initialMetricValue = assertMetricsContainTotalSendMetric().withInitialValue();

        // when
        hermes.api().publish(topic.getQualifiedName(), "hello world");
        hermes.api().publish(topic.getQualifiedName(), "hello world");

        // then
        await().atMost(10, TimeUnit.SECONDS).until(() ->
                assertMetricsContainTotalSendMetric().withValue(initialMetricValue + 2.0));
    }

    PrometheusMetricsAssertion.PrometheusMetricAssertion assertMetricsContainTotalSendMetric() {
        return assertThatMetrics(hermes.api()
                .getFrontendMetrics().expectStatus().isOk()
                .expectBody(String.class).returnResult().getResponseBody())
                .contains("hermes_frontend_kafka_producer_ack_leader_record_send_total")
                .withLabels(
                        "storageDc", "dc",
                        "sender", "default"
                );
    }


}
