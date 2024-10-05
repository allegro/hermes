package pl.allegro.tech.hermes.mock;

import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.http.RequestMethod.POST;
import static pl.allegro.tech.hermes.mock.HermesMockHelper.startsWith;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.matching.ValueMatcher;
import java.util.function.Predicate;
import org.apache.avro.Schema;
import pl.allegro.tech.hermes.mock.matching.ContentMatchers;

public class HermesMock {

  private static final String APPLICATION_JSON = "application/json";
  private static final String AVRO_BINARY = "avro/binary";

  private final WireMockServer wireMockServer;

  private final HermesMockDefine hermesMockDefine;
  private final HermesMockExpect hermesMockExpect;
  private final HermesMockQuery hermesMockQuery;
  private final HermesMockHelper hermesMockHelper;

  private HermesMock(int port, int awaitSeconds, ObjectMapper objectMapper) {
    wireMockServer = new WireMockServer(port);

    hermesMockHelper = new HermesMockHelper(wireMockServer, objectMapper);
    hermesMockDefine = new HermesMockDefine(hermesMockHelper);
    hermesMockExpect = new HermesMockExpect(hermesMockHelper, awaitSeconds);
    hermesMockQuery = new HermesMockQuery(hermesMockHelper);
  }

  public HermesMockDefine define() {
    return hermesMockDefine;
  }

  public HermesMockExpect expect() {
    return hermesMockExpect;
  }

  public HermesMockQuery query() {
    return hermesMockQuery;
  }

  public void resetReceivedRequest() {
    wireMockServer.resetRequests();
  }

  public <T> void resetReceivedAvroRequests(
      String topicName, Schema schema, Class<T> clazz, Predicate<T> predicate) {
    ValueMatcher<Request> valueMatcher =
        ContentMatchers.matchAvro(hermesMockHelper, predicate, schema, clazz);
    resetReceivedRequests(topicName, AVRO_BINARY, valueMatcher);
  }

  public <T> void resetReceivedJsonRequests(
      String topicName, Class<T> clazz, Predicate<T> predicate) {
    ValueMatcher<com.github.tomakehurst.wiremock.http.Request> valueMatcher =
        ContentMatchers.matchJson(hermesMockHelper, predicate, clazz);
    resetReceivedRequests(topicName, APPLICATION_JSON, valueMatcher);
  }

  private void resetReceivedRequests(
      String topicName,
      String contentType,
      ValueMatcher<com.github.tomakehurst.wiremock.http.Request> valueMatcher) {
    RequestPattern requestPattern =
        RequestPatternBuilder.newRequestPattern(POST, urlEqualTo("/topics/" + topicName))
            .withHeader("Content-Type", startsWith(contentType))
            .andMatching(valueMatcher)
            .build();
    wireMockServer.removeServeEventsMatching(requestPattern);
  }

  public void resetMappings() {
    wireMockServer.resetMappings();
  }

  public void start() {
    wireMockServer.start();
  }

  public void stop() {
    wireMockServer.stop();
  }

  public static class Builder {
    private int port;
    private int awaitSeconds;
    private ObjectMapper objectMapper;

    public Builder() {
      port = wireMockConfig().portNumber();
      awaitSeconds = 5;
      objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    public HermesMock.Builder withPort(int port) {
      this.port = port;
      return this;
    }

    public HermesMock.Builder withObjectMapper(ObjectMapper objectMapper) {
      this.objectMapper = objectMapper;
      return this;
    }

    public HermesMock.Builder withAwaitSeconds(int awaitSeconds) {
      this.awaitSeconds = awaitSeconds;
      return this;
    }

    public HermesMock build() {
      if (port == 0) {
        port = wireMockConfig().dynamicPort().portNumber();
      }
      return new HermesMock(port, awaitSeconds, objectMapper);
    }
  }
}
