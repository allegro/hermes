package pl.allegro.tech.hermes.mock;

import static pl.allegro.tech.hermes.mock.exchange.Response.Builder.aResponse;

import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.matching.ValueMatcher;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import java.util.function.Predicate;
import org.apache.avro.Schema;
import pl.allegro.tech.hermes.mock.exchange.Response;
import pl.allegro.tech.hermes.mock.matching.ContentMatchers;
import wiremock.org.apache.hc.core5.http.HttpStatus;

public class HermesMockDefine {
  private static final String APPLICATION_JSON = "application/json";
  private static final String AVRO_BINARY = "avro/binary";
  private final HermesMockHelper hermesMockHelper;

  public HermesMockDefine(HermesMockHelper hermesMockHelper) {
    this.hermesMockHelper = hermesMockHelper;
  }

  public StubMapping jsonTopic(String topicName) {
    return jsonTopic(topicName, HttpStatus.SC_CREATED);
  }

  public StubMapping jsonTopic(String topicName, int statusCode) {
    return addTopic(topicName, aResponse().withStatusCode(statusCode).build(), APPLICATION_JSON);
  }

  public StubMapping jsonTopic(String topicName, Response response) {
    return addTopic(topicName, response, APPLICATION_JSON);
  }

  public <T> StubMapping jsonTopic(
      String topicName, Response response, Class<T> clazz, Predicate<T> predicate) {
    ValueMatcher<Request> jsonMatchesPattern =
        ContentMatchers.matchJson(hermesMockHelper, predicate, clazz);
    return addTopic(topicName, response, APPLICATION_JSON, jsonMatchesPattern);
  }

  public StubMapping avroTopic(String topicName) {
    return avroTopic(topicName, HttpStatus.SC_CREATED);
  }

  public StubMapping avroTopic(String topicName, int statusCode) {
    return addTopic(topicName, aResponse().withStatusCode(statusCode).build(), AVRO_BINARY);
  }

  public StubMapping avroTopic(String topicName, Response response) {
    return addTopic(topicName, response, AVRO_BINARY);
  }

  public <T> StubMapping avroTopic(
      String topicName, Response response, Schema schema, Class<T> clazz, Predicate<T> predicate) {
    ValueMatcher<Request> avroMatchesPattern =
        ContentMatchers.matchAvro(hermesMockHelper, predicate, schema, clazz);
    return addTopic(topicName, response, AVRO_BINARY, avroMatchesPattern);
  }

  public void removeStubMapping(StubMapping stubMapping) {
    hermesMockHelper.removeStubMapping(stubMapping);
  }

  private StubMapping addTopic(String topicName, Response response, String contentType) {
    return hermesMockHelper.addStub(topicName, response, contentType);
  }

  private StubMapping addTopic(
      String topicName, Response response, String contentType, ValueMatcher<Request> valueMatcher) {
    return hermesMockHelper.addStub(topicName, response, contentType, valueMatcher);
  }
}
