package pl.allegro.tech.hermes.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.http.HttpStatus;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;

public class HermesMock {
    private WireMockServer wireMockServer;
    private ObjectMapper objectMapper = new ObjectMapper();
    private int awaitSeconds = 5;

    public HermesMock() {
        wireMockServer = new WireMockServer();
    }

    public HermesMock(int port) {
        wireMockServer = new WireMockServer(port);
    }

    public void setAwaitSeconds(int awaitSeconds) {
        this.awaitSeconds = awaitSeconds;
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void addTopic(String topicName) {
        addTopic(topicName, HttpStatus.SC_CREATED);
    }

    public void addAvroTopic(String topicName) {
        addAvroTopic(topicName, HttpStatus.SC_CREATED);
    }

    public void addTopic(String topicName, int statusCode) {
        addTopic(topicName, false, aResponse().withStatus(statusCode));
    }

    public void addAvroTopic(String topicName, int statusCode) {
        addTopic(topicName, true, aResponse().withStatus(statusCode));
    }

    public void addTopic(String topicName, Boolean isAvro, ResponseDefinitionBuilder responseDefinitionBuilder) {
        if (isAvro) {
            wireMockServer.stubFor(get(urlPathMatching("/topics/" + topicName))
                    .willReturn(responseDefinitionBuilder
                            .withHeader("Content-Type", "avro/binary")
                            .withHeader("Hermes-Message-Id", UUID.randomUUID().toString())
                    )
            );
        } else {
            wireMockServer.stubFor(get(urlPathMatching("/topics/" + topicName))
                    .willReturn(responseDefinitionBuilder
                            .withHeader("Content-Type", "application/json")
                            .withHeader("Hermes-Message-Id", UUID.randomUUID().toString())
                    )
            );
        }
    }

    public void expectSingleMessageOnTopic(String topicName) {
        expectMessagesOnTopic(1, topicName);
    }

    public void expectMessagesOnTopic(int count, String topicName) {
        try {
            await().atMost(awaitSeconds, SECONDS).until(() ->
                    wireMockServer.verify(count, postRequestedFor(urlEqualTo("/topics/" + topicName)))
            );
        } catch (Exception ex) {
            throw new HermesMockException("Hermes mock did not receive " + count + " messages. " + ex.getMessage());
        }
    }

    public <T> void expectSingleMessageOnTopicAs(String topicName, Class<T> clazz) {
        expectMessagesOnTopicAs(1, topicName, clazz);
    }

    public <T> void expectMessagesOnTopicAs(int count, String topicName, Class<T> clazz) {
        try {
            await().atMost(awaitSeconds, SECONDS).until(() ->
                    wireMockServer.verify(count, postRequestedFor(urlEqualTo("/topics/" + topicName)))
            );
        } catch (Exception ex) {
            throw new HermesMockException("Hermes mock did not received " + count + " messages. " + ex.getMessage());
        }
        List<T> allMessages = getAllMessagesAs(topicName, clazz);
        if (allMessages.size() != count) {
            throw new HermesMockException("Hermes mock did not received " + count + " messages, got " + allMessages.size());
        }
    }

    public void expectSingleAvroMessageOnTopic(String topicName, Schema schema) {
        expectAvroMessagesOnTopic(1, topicName, schema);
    }

    public void expectAvroMessagesOnTopic(int count, String topicName, Schema schema) {
        try {
            await().atMost(awaitSeconds, SECONDS).until(() ->
                    wireMockServer.verify(count, postRequestedFor(urlEqualTo("/topics/" + topicName)))
            );
        } catch (Exception ex) {
            throw new HermesMockException("Hermes mock did not received " + count + " messages. " + ex.getMessage());
        }
        if (getAllAvroMessagesAs(topicName, schema).size() != count) {
            throw new HermesMockException("Hermes mock did not received " + count + " messages, got " + getAllAvroMessagesAs(topicName, schema).size());
        }
    }

    public List<LoggedRequest> getAllRequests() {
        return wireMockServer.findAll(postRequestedFor(urlPathMatching(("/topics/"))));
    }

    public List<LoggedRequest> getAllRequests(String topicName) {
        return wireMockServer.findAll(postRequestedFor(urlEqualTo("/topics/" + topicName)));
    }

    public <T> List<T> getAllMessagesAs(String topicName, Class<T> clazz) {
        return getAllRequests(topicName).stream()
                .map(req -> deserialize(req, clazz))
                .collect(toList());
    }

    public List<byte[]> getAllAvroMessagesAs(String topicName, Schema schema) {
        return getAllRequests(topicName).stream()
                .map(req -> deserializeAvro(req, schema))
                .collect(toList());
    }

    public Optional<LoggedRequest> getLastRequest(String topicName) {
        return getAllRequests(topicName).stream().findFirst();
    }

    public <T> Optional<T> getLastMessageAs(String topicName, Class<T> clazz) {
        return getLastRequest(topicName)
                .map(req -> deserialize(req, clazz));
    }

    public void resetReceivedRequest() {
        wireMockServer.resetRequests();
    }

    public void start() {
        wireMockServer.start();
    }

    public void stop() {
        wireMockServer.stop();
    }

    private <T> T deserialize(LoggedRequest request, Class<T> clazz) {
        try {
            return objectMapper.readValue(request.getBodyAsString(), clazz);
        } catch (IOException ex) {
            throw new HermesMockException("Cannot read body " + request.getBodyAsString() + " as " + clazz.getSimpleName());
        }
    }

    private byte[] deserializeAvro(LoggedRequest request, Schema schema) {
        try {
            return convertToAvro(readJson(request.getBody(), schema), schema);
        } catch (IOException | AvroRuntimeException ex) {
            throw new HermesMockException("Failed to convert to AVRO." + ex.getMessage());
        }
    }

    private GenericData.Record readJson(byte[] bytes, Schema schema) throws IOException {
        InputStream input = new ByteArrayInputStream(bytes);
        Decoder decoder = DecoderFactory.get().jsonDecoder(schema, input);
        return new GenericDatumReader<GenericData.Record>(schema).read(null, decoder);
    }

    private byte[] convertToAvro(GenericData.Record jsonData, Schema schema) throws IOException {
        GenericDatumWriter<GenericData.Record> writer = new GenericDatumWriter<>(schema);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().binaryEncoder(outputStream, null);
        writer.write(jsonData, encoder);
        encoder.flush();
        return outputStream.toByteArray();
    }
}
