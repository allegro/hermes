package pl.allegro.tech.hermes.mock;

import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.matching.ValueMatcher;
import org.apache.avro.Schema;
import org.apache.http.HttpStatus;
import pl.allegro.tech.hermes.mock.exchange.Response;
import pl.allegro.tech.hermes.mock.matching.ContentMatchers;

import java.util.function.Predicate;

import static pl.allegro.tech.hermes.mock.exchange.Response.Builder.aResponse;

public class HermesMockDefine {
    private static final String APPLICATION_JSON = "application/json";
    private static final String AVRO_BINARY = "avro/binary";
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
        addTopic(topicName, aResponse().withStatusCode(statusCode).build(), APPLICATION_JSON);
    }

    public void jsonTopic(String topicName, Response response) {
        addTopic(topicName, response, APPLICATION_JSON);
    }

    public void avroTopic(String topicName, int statusCode) {
        addTopic(topicName, aResponse().withStatusCode(statusCode).build(), AVRO_BINARY);
    }

    public void avroTopic(String topicName, Response response) {
        addTopic(topicName, response, AVRO_BINARY);
    }

    public <T> void avroTopic(String topicName, Response response, Schema schema, Class<T> clazz, Predicate<T> predicate) {
        ValueMatcher<Request> avroMatchesPattern = ContentMatchers.matchAvro(hermesMockHelper, predicate, schema, clazz);
        addTopic(topicName, response, AVRO_BINARY, avroMatchesPattern);
    }

    public <T> void jsonTopic(String topicName, Response response, Class<T> clazz, Predicate<T> predicate) {
        ValueMatcher<Request> jsonMatchesPattern = ContentMatchers.matchJson(hermesMockHelper, predicate, clazz);
        addTopic(topicName, response, APPLICATION_JSON, jsonMatchesPattern);
    }

    private void addTopic(String topicName, Response response, String contentType) {
        hermesMockHelper.addStub(topicName, response, contentType);
    }

    private void addTopic(String topicName, Response response, String contentType, ValueMatcher<Request> valueMatcher) {
        hermesMockHelper.addStub(topicName, response, contentType, valueMatcher);
    }
}
