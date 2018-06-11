package pl.allegro.tech.hermes.mock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.RequestPatternBuilder;
import org.apache.avro.Schema;

import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static java.util.stream.Collectors.toList;

class HermesMockQuery {
    private WireMockServer wireMockServer;
    private HermesMockHelper hermesMockHelper;

    public HermesMockQuery(WireMockServer wireMockServer, HermesMockHelper hermesMockHelper) {
        this.wireMockServer = wireMockServer;
        this.hermesMockHelper = hermesMockHelper;
    }

    public List<Request> allRequests() {
        return wireMockServer.findAll(postRequestedFor(urlPathMatching(("/topics/")))).stream()
                .map(Request::new)
                .collect(toList());
    }

    public List<Request> allRequestsOnTopic(String topicName) {
        RequestPatternBuilder matcher = postRequestedFor(urlEqualTo("/topics/" + topicName));
        return wireMockServer.findAll(matcher).stream()
                .map(Request::new)
                .collect(toList());
    }

    public <T> List<T> allJsonMessagesAs(String topicName, Class<T> clazz) {
        return allRequestsOnTopic(topicName).stream()
                .map(req -> hermesMockHelper.deserializeJson(req.getBody(), clazz))
                .collect(toList());
    }

    public List<byte[]> allAvroRawMessages(String topicName) {
        return allRequestsOnTopic(topicName).stream()
                .map(Request::getBody)
                .collect(toList());
    }

    public <T> List<T> allAvroMessagesAs(String topicName, Schema schema, Class<T> clazz) {
        return allRequestsOnTopic(topicName).stream()
                .map(req -> hermesMockHelper.deserializeAvro(req, schema, clazz))
                .collect(toList());
    }

    public Optional<Request> lastRequest(String topicName) {
        return allRequestsOnTopic(topicName).stream().findFirst();
    }

    public <T> Optional<T> lastJsonMessageAs(String topicName, Class<T> clazz) {
        return lastRequest(topicName)
                .map(req -> hermesMockHelper.deserializeJson(req.getBody(), clazz));
    }

    public <T> Optional<byte[]> lastAvroRawMessage(String topicName) {
        return lastRequest(topicName)
                .map(Request::getBody);
    }

    public <T> Optional<T> lastAvroMessageAs(String topicName, Schema schema, Class<T> clazz) {
        return lastRequest(topicName)
                .map(req -> hermesMockHelper.deserializeAvro(req, schema, clazz));
    }
}
