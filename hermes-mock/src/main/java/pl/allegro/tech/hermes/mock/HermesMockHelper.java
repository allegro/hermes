package pl.allegro.tech.hermes.mock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import com.github.tomakehurst.wiremock.matching.ValueMatcher;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import pl.allegro.tech.hermes.mock.exchange.Request;
import pl.allegro.tech.hermes.mock.exchange.Response;
import pl.allegro.tech.hermes.mock.matching.StartsWithPattern;
import tech.allegro.schema.json2avro.converter.JsonAvroConverter;

public class HermesMockHelper {
  private final WireMockServer wireMockServer;
  private final ObjectMapper objectMapper;

  public HermesMockHelper(WireMockServer wireMockServer, ObjectMapper objectMapper) {
    this.wireMockServer = wireMockServer;
    this.objectMapper = objectMapper;
  }

  private static Integer toIntMilliseconds(Duration duration) {
    return Optional.ofNullable(duration).map(Duration::toMillis).map(Math::toIntExact).orElse(null);
  }

  public static StringValuePattern startsWith(String value) {
    return new StartsWithPattern(value);
  }

  public <T> T deserializeJson(byte[] content, Class<T> clazz) {
    try {
      return objectMapper.readValue(content, clazz);
    } catch (IOException ex) {
      throw new HermesMockException(
          "Cannot read body " + content.toString() + " as " + clazz.getSimpleName(), ex);
    }
  }

  public <T> T deserializeAvro(Request request, Schema schema, Class<T> clazz) {
    return deserializeAvro(request.getBody(), schema, clazz);
  }

  public <T> T deserializeAvro(byte[] raw, Schema schema, Class<T> clazz) {
    try {
      byte[] json = new JsonAvroConverter().convertToJson(raw, schema);
      return deserializeJson(json, clazz);
    } catch (RuntimeException ex) {
      throw new HermesMockException(
          "Cannot decode body " + raw + " to " + clazz.getSimpleName(), ex);
    }
  }

  public void validateAvroSchema(byte[] raw, Schema schema) {
    try {
      BinaryDecoder binaryDecoder = DecoderFactory.get().binaryDecoder(raw, null);
      new GenericDatumReader<>(schema).read(null, binaryDecoder);
    } catch (IOException e) {
      throw new HermesMockException("Cannot convert bytes as " + schema.getName());
    }
  }

  public List<LoggedRequest> findAll(RequestPatternBuilder requestPatternBuilder) {
    return wireMockServer.findAll(requestPatternBuilder);
  }

  public void verifyRequest(int count, String topicName) {
    wireMockServer.verify(count, postRequestedFor(urlEqualTo("/topics/" + topicName)));
  }

  public StubMapping addStub(String topicName, Response response, String contentType) {
    return wireMockServer.stubFor(
        post(urlEqualTo("/topics/" + topicName))
            .withHeader("Content-Type", startsWith(contentType))
            .willReturn(
                aResponse()
                    .withStatus(response.getStatusCode())
                    .withHeader("Hermes-Message-Id", UUID.randomUUID().toString())
                    .withFixedDelay(toIntMilliseconds(response.getFixedDelay()))));
  }

  public StubMapping addStub(
      String topicName,
      Response response,
      String contentType,
      ValueMatcher<com.github.tomakehurst.wiremock.http.Request> valueMatcher) {
    return wireMockServer.stubFor(
        post(urlEqualTo("/topics/" + topicName))
            .andMatching(valueMatcher)
            .withHeader("Content-Type", startsWith(contentType))
            .willReturn(
                aResponse()
                    .withStatus(response.getStatusCode())
                    .withHeader("Hermes-Message-Id", UUID.randomUUID().toString())
                    .withFixedDelay(toIntMilliseconds(response.getFixedDelay()))));
  }

  public void removeStubMapping(StubMapping stubMapping) {
    wireMockServer.removeStubMapping(stubMapping);
  }
}
