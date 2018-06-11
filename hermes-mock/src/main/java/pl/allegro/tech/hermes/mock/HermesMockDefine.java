package pl.allegro.tech.hermes.mock;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.apache.http.HttpStatus;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

class HermesMockDefine {
    private WireMockServer wireMockServer;

    public HermesMockDefine(WireMockServer wireMockServer) {
        this.wireMockServer = wireMockServer;
    }

    public void jsonTopic(String topicName) {
        jsonTopic(topicName, HttpStatus.SC_CREATED);
    }

    public void avroTopic(String topicName) {
        avroTopic(topicName, HttpStatus.SC_CREATED);
    }

    public void jsonTopic(String topicName, int statusCode) {
        addTopic(topicName, statusCode, "application/json");
    }

    public void avroTopic(String topicName, int statusCode) {
        addTopic(topicName, statusCode, "avro/binary");
    }

    private void addTopic(String topicName, int statusCode, String contentType) {
        wireMockServer.stubFor(get(urlPathMatching("/topics/" + topicName))
                .willReturn(aResponse()
                        .withStatus(statusCode)
                        .withHeader("Content-Type", contentType)
                        .withHeader("Hermes-Message-Id", UUID.randomUUID().toString())
                )
        );
    }
}
