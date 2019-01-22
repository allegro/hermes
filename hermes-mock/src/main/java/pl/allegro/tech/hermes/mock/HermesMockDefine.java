package pl.allegro.tech.hermes.mock;

import org.apache.http.HttpStatus;

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
        addTopic(topicName, statusCode, "application/json");
    }

    public void avroTopic(String topicName, int statusCode) {
        addTopic(topicName, statusCode, "avro/binary");
    }

    private void addTopic(String topicName, int statusCode, String contentType) {
        hermesMockHelper.addStub(topicName, statusCode, contentType);
    }
}
