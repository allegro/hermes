package pl.allegro.tech.hermes.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import tech.allegro.schema.json2avro.converter.JsonAvroConverter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class HermesMockHelper {
    private final WireMockServer wireMockServer;
    private final ObjectMapper objectMapper;

    public HermesMockHelper(WireMockServer wireMockServer, ObjectMapper objectMapper) {
        this.wireMockServer = wireMockServer;
        this.objectMapper = objectMapper;
    }

    public <T> T deserializeJson(byte[] content, Class<T> clazz) {
        try {
            return objectMapper.readValue(content, clazz);
        } catch (IOException ex) {
            throw new HermesMockException("Cannot read body " + content.toString() + " as " + clazz.getSimpleName());
        }
    }

    public <T> T deserializeAvro(Request request, Schema schema, Class<T> clazz) {
        try {
            byte[] json = new JsonAvroConverter().convertToJson(request.getBody(), schema);
            return deserializeJson(json, clazz);
        } catch (AvroRuntimeException ex) {
            throw new HermesMockException("Cannot convert body " + request.getBody().toString() + " as " + clazz.getSimpleName());
        }
    }

    public void validateAvroSchema(byte[] raw, Schema schema) {
        try {
            BinaryDecoder binaryDecoder = DecoderFactory.get().binaryDecoder(raw, null);
            new GenericDatumReader<>(schema).read(null, binaryDecoder);
        } catch (IOException e) {
            throw new HermesMockException("Cannot convert raw bytes as " + schema.getName());
        }
    }

    public List<LoggedRequest> findAll(RequestPatternBuilder requestPatternBuilder) {
        return wireMockServer.findAll(requestPatternBuilder);
    }

    public void verifyRequest(int count, String topicName) {
        wireMockServer.verify(count, postRequestedFor(urlEqualTo("/topics/" + topicName)));
    }

    public void addStub(String topicName, int statusCode, String contentType) {
        wireMockServer.stubFor(post(urlEqualTo("/topics/" + topicName))
                .willReturn(aResponse()
                        .withStatus(statusCode)
                        .withHeader("Content-Type", contentType)
                        .withHeader("Hermes-Message-Id", UUID.randomUUID().toString())
                )
        );
    }
}
