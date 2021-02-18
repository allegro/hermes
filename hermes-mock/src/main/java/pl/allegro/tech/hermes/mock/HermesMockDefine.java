package pl.allegro.tech.hermes.mock;

import org.apache.http.HttpStatus;
import pl.allegro.tech.hermes.mock.Response.Builder;

import static pl.allegro.tech.hermes.mock.Response.Builder.aResponse;

public class HermesMockDefine {
    private final HermesMockHelper hermesMockHelper;

    public HermesMockDefine(HermesMockHelper hermesMockHelper) {
        this.hermesMockHelper = hermesMockHelper;
    }

    public void jsonTopic(String topicName) {
        jsonTopic(topicName, HttpStatus.SC_CREATED);
    }

    public void avroTopic(String topicName) {
        avroTopic(topicName, HttpStatus.SC_CREATED);
    }

    public void jsonTopic(String topicName, int statusCode) {
        addTopic(topicName, aResponse().withStatusCode(statusCode).build(), "application/json");
    }

    public void avroTopic(String topicName, int statusCode) {
        addTopic(topicName, aResponse().withStatusCode(statusCode).build(), "avro/binary");
    }

    private void addTopic(String topicName, Response response, String contentType) {
        hermesMockHelper.addStub(topicName, response, contentType);
    }
}
