package pl.allegro.tech.hermes.mock;

import static pl.allegro.tech.hermes.mock.exchange.Response.Builder.aResponse;

import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.matching.ValueMatcher;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import java.util.function.Predicate;
import org.apache.avro.Schema;
import pl.allegro.tech.hermes.api.TopicName;
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

  private void validateTopicName(String topicName) {
    TopicName.fromQualifiedName(topicName);
  }

  /**
   * Defines a JSON topic.
   *
   * @param topicName qualified topic name in format {@code [a-zA-Z0-9_.-]+.[a-zA-Z0-9_.-]+},
   *     where group and topic name must match the pattern {@code [a-zA-Z0-9_.-]+}, e.g.
   *     "pl.allegro.public.MyTopic"
   * @return stub mapping for the defined topic
   * @throws IllegalArgumentException if topic name is not in qualified format
   */
  public StubMapping jsonTopic(String topicName) {
    validateTopicName(topicName);
    return jsonTopic(topicName, HttpStatus.SC_CREATED);
  }

  /**
   * Defines a JSON topic that responds with a given status code.
   *
   * @param topicName qualified topic name in format {@code [a-zA-Z0-9_.-]+.[a-zA-Z0-9_.-]+}
   * @param statusCode HTTP status code to return when publishing to this topic
   * @return stub mapping for the defined topic
   * @throws IllegalArgumentException if topic name is not in qualified format
   */
  public StubMapping jsonTopic(String topicName, int statusCode) {
    validateTopicName(topicName);
    return addTopic(topicName, aResponse().withStatusCode(statusCode).build(), APPLICATION_JSON);
  }

  /**
   * Defines a JSON topic that responds with a given response.
   *
   * @param topicName qualified topic name in format {@code [a-zA-Z0-9_.-]+.[a-zA-Z0-9_.-]+}
   * @param response response to return when publishing to this topic
   * @return stub mapping for the defined topic
   * @throws IllegalArgumentException if topic name is not in qualified format
   */
  public StubMapping jsonTopic(String topicName, Response response) {
    validateTopicName(topicName);
    return addTopic(topicName, response, APPLICATION_JSON);
  }

  /**
   * Defines a JSON topic with a predicate to match request by field.
   *
   * @param topicName qualified topic name in format {@code [a-zA-Z0-9_.-]+.[a-zA-Z0-9_.-]+}
   * @param response response to return when publishing to this topic
   * @param clazz class type to deserialize JSON messages to
   * @param predicate predicate to match messages
   * @return stub mapping for the defined topic
   * @throws IllegalArgumentException if topic name is not in qualified format
   */
  public <T> StubMapping jsonTopic(
      String topicName, Response response, Class<T> clazz, Predicate<T> predicate) {
    validateTopicName(topicName);
    ValueMatcher<Request> jsonMatchesPattern =
        ContentMatchers.matchJson(hermesMockHelper, predicate, clazz);
    return addTopic(topicName, response, APPLICATION_JSON, jsonMatchesPattern);
  }

  /**
   * Defines an Avro topic.
   *
   * @param topicName qualified topic name in format {@code [a-zA-Z0-9_.-]+.[a-zA-Z0-9_.-]+}
   * @return stub mapping for the defined topic
   * @throws IllegalArgumentException if topic name is not in qualified format
   */
  public StubMapping avroTopic(String topicName) {
    validateTopicName(topicName);
    return avroTopic(topicName, HttpStatus.SC_CREATED);
  }

  /**
   * Defines an Avro topic that responds with a given status code.
   *
   * @param topicName qualified topic name in format {@code [a-zA-Z0-9_.-]+.[a-zA-Z0-9_.-]+}
   * @param statusCode HTTP status code to return when publishing to this topic
   * @return stub mapping for the defined topic
   * @throws IllegalArgumentException if topic name is not in qualified format
   */
  public StubMapping avroTopic(String topicName, int statusCode) {
    validateTopicName(topicName);
    return addTopic(topicName, aResponse().withStatusCode(statusCode).build(), AVRO_BINARY);
  }

  /**
   * Defines an Avro topic that responds with a given response.
   *
   * @param topicName qualified topic name in format {@code [a-zA-Z0-9_.-]+.[a-zA-Z0-9_.-]+}
   * @param response response to return when publishing to this topic
   * @return stub mapping for the defined topic
   * @throws IllegalArgumentException if topic name is not in qualified format
   */
  public StubMapping avroTopic(String topicName, Response response) {
    validateTopicName(topicName);
    return addTopic(topicName, response, AVRO_BINARY);
  }

  /**
   * Defines an Avro topic with a predicate to match request by field in schema.
   *
   * @param topicName qualified topic name in format {@code [a-zA-Z0-9_.-]+.[a-zA-Z0-9_.-]+}
   * @param response response to return when publishing to this topic
   * @param schema Avro schema for the messages
   * @param clazz class type to deserialize Avro messages to
   * @param predicate predicate to match messages
   * @return stub mapping for the defined topic
   * @throws IllegalArgumentException if topic name is not in qualified format
   */
  public <T> StubMapping avroTopic(
      String topicName, Response response, Schema schema, Class<T> clazz, Predicate<T> predicate) {
    validateTopicName(topicName);
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
