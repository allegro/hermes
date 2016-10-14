package pl.allegro.tech.hermes.integration.helper;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import pl.allegro.tech.hermes.integration.env.EnvironmentAware;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

public class GraphiteEndpoint implements EnvironmentAware {

    private static final String TIMESTAMP = "1396860420";

    private static final String TOPIC_RESPONSE
            = "[ "
            + "{\"target\": \"sumSeries(stats.tech.hermes.producer.*.meter.TOPIC.m1_rate)\", \"datapoints\": "
            + "[[RATE, TIMESTAMP]]},"
            + "{\"target\": \"sumSeries(stats.tech.hermes.consumer.*.meter.TOPIC.m1_rate)\", \"datapoints\": "
            + "[[DELIVERY, TIMESTAMP]]}"
            + "]";

    private static final String SUBSCRIPTION_RESPONSE
            = "[ "
            + "{\"target\": \"sumSeries(stats.tech.hermes.consumer.*.meter.SUBSCRIPTION.m1_rate)\", \"datapoints\": "
            + "[[RATE, TIMESTAMP]]}"
            + "]";

    private static final String TOPIC_URL_PATTERN = "/.*sumSeries%28stats.tech.hermes\\." +
            "(consumer|producer)\\.%2A\\.meter\\.[^\\.]*\\.[^\\.]*\\.m1_rate%29.*";

    private static final String SUBSCRIPTION_URL_PATTERN = "/.*sumSeries%28stats.tech.hermes\\." +
            "consumer\\.%2A\\.meter\\.[^\\.]*\\.[^\\.]*\\.[^\\.]*\\.m1_rate%29.*";

    private final WireMock graphiteListener;

    public GraphiteEndpoint(WireMockServer graphiteMock) {
        this.graphiteListener = new WireMock("localhost", graphiteMock.port());
    }

    public void returnMetricForTopic(String group, String topic, int rate, int deliveryRate) {
        String response = TOPIC_RESPONSE.replaceAll("TOPIC", group + "." + topic)
                .replaceAll("RATE", Integer.toString(rate))
                .replaceAll("DELIVERY", Integer.toString(deliveryRate))
                .replaceAll("TIMESTAMP", TIMESTAMP);
        graphiteListener.register(get(urlMatching(TOPIC_URL_PATTERN))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(response)));
    }

    public void returnMetricForSubscription(String group, String topic, String subscription, int rate) {
        String response = SUBSCRIPTION_RESPONSE.replaceAll("SUBSCRIPTION", group + "." + topic + "." + subscription)
                .replaceAll("RATE", Integer.toString(rate))
                .replaceAll("TIMESTAMP", TIMESTAMP);
        graphiteListener.register(get(urlMatching(SUBSCRIPTION_URL_PATTERN))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(response)));
    }

    public void returnServerErrorForAllTopics() {
        graphiteListener.register(get(urlMatching(TOPIC_URL_PATTERN))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                )
        );
    }

}
