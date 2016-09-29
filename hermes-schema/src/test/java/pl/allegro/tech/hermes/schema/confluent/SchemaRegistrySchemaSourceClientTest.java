package pl.allegro.tech.hermes.schema.confluent;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.UrlMatchingStrategy;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import pl.allegro.tech.hermes.api.SchemaSource;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.schema.CouldNotRemoveSchemaException;
import pl.allegro.tech.hermes.schema.InvalidSchemaException;
import pl.allegro.tech.hermes.schema.SchemaRepositoryServerException;
import pl.allegro.tech.hermes.schema.SchemaSourceClient;
import pl.allegro.tech.hermes.schema.SchemaVersion;
import pl.allegro.tech.hermes.test.helper.util.Ports;

import javax.ws.rs.client.ClientBuilder;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.assertj.core.api.Assertions.assertThat;

public class SchemaRegistrySchemaSourceClientTest {

    private static final int PORT = Ports.nextAvailable();

    private static final String SCHEMA_REGISTRY_CONTENT_TYPE = "application/vnd.schemaregistry.v1+json";
    private static final String TOPIC_NAME = "group.topic";
    private static final TopicName topic = TopicName.fromQualifiedName(TOPIC_NAME);
    private static final SchemaSource schemaSource = SchemaSource.valueOf("{}");

    private static final SchemaSourceClient client = new SchemaRegistrySchemaSourceClient(
            ClientBuilder.newClient(),
            URI.create("http://localhost:" + PORT)
    );

    @ClassRule
    public static final WireMockRule wireMock = new WireMockRule(
            wireMockConfig().port(PORT).usingFilesUnderClasspath("schema-repo-stub")
    );

    @Before
    public void initialize() {
        wireMock.resetRequests();
        wireMock.resetMappings();
        wireMock.resetScenarios();
    }

    @Test
    public void shouldRegisterSchemaSource() {
        // given
        stubFor(post(schemaVersionsUrl(TOPIC_NAME)).willReturn(okResponse()));

        // when
        client.registerSchemaSource(topic, SchemaSource.valueOf("{}"));

        // then
        verify(1, postRequestedFor(schemaVersionsUrl(TOPIC_NAME))
                .withHeader("Content-type", equalTo(SCHEMA_REGISTRY_CONTENT_TYPE))
                .withRequestBody(equalTo("{\"schema\":\"{}\"}")));
    }

    @Test
    public void shouldThrowInvalidSchemaExceptionForInvalidSchemaRegistration() {
        // given
        stubFor(post(schemaVersionsUrl(TOPIC_NAME)).willReturn(badRequestResponse()));

        // when
        catchException(client).registerSchemaSource(topic, schemaSource);

        // then
        assertThat((Throwable) caughtException()).isInstanceOf(InvalidSchemaException.class);
    }

    @Test
    public void shouldThrowSchemaRepositoryServerExceptionForServerInternalErrorResponseWhenRegisteringSchema() {
        // given
        stubFor(post(schemaVersionsUrl(TOPIC_NAME)).willReturn(internalErrorResponse()));

        // when
        catchException(client).registerSchemaSource(topic, schemaSource);

        // then
        assertThat((Throwable) caughtException()).isInstanceOf(SchemaRepositoryServerException.class);
    }

    @Test
    public void shouldFetchSchemaAtVersion() {
        // given
        stubFor(get(getSchemaVersion(TOPIC_NAME, 5))
                .willReturn(okResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"subject\":\"group.topic\", \"id\":100, \"version\":5, \"schema\":\"{}\"}")));

        // when
        Optional<SchemaSource> schemaAtVersion5 = client.getSchemaSource(topic, SchemaVersion.valueOf(5));

        // then
        assertThat(schemaAtVersion5.get()).isEqualTo(SchemaSource.valueOf("{}"));
    }

    @Test
    public void shouldFetchLatestSchemaVersion() {
        // given
        stubFor(get(getSchemaLatestVersionUrl(TOPIC_NAME))
                .willReturn(okResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"subject\":\"group.topic\", \"id\":3, \"version\":2, \"schema\":\"{}\"}")));

        // when
        Optional<SchemaSource> latestSchema = client.getLatestSchemaSource(topic);

        // then
        assertThat(latestSchema.get()).isEqualTo(SchemaSource.valueOf("{}"));
    }

    @Test
    public void shouldReturnAllSchemaVersions() {
        // given
        stubFor(get(schemaVersionsUrl(TOPIC_NAME))
                .willReturn(okResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[3,6,2]")));

        // when
        List<SchemaVersion> versions = client.getVersions(topic);

        // then
        Assertions.assertThat(versions).contains(SchemaVersion.valueOf(3), SchemaVersion.valueOf(6), SchemaVersion.valueOf(2));
    }

    @Test
    public void shouldReturnEmptyListOnNotRegisteredSubject() {
        // given
        stubFor(get(schemaVersionsUrl(TOPIC_NAME)).willReturn(notFoundResponse()));

        // when
        List<SchemaVersion> versions = client.getVersions(topic);

        // then
        Assertions.assertThat(versions).isEmpty();
    }

    @Test
    public void shouldDeleteAllSchemaVersions() {
        // given
        stubFor(delete(schemaVersionsUrl(TOPIC_NAME)).willReturn(okResponse()));

        // when
        client.deleteAllSchemaSources(topic);

        // then
        verify(1, deleteRequestedFor(schemaVersionsUrl(TOPIC_NAME)));
    }

    @Test
    public void shouldThrowExceptionForMethodNotAllowedResponseWhenDeletingSchema() {
        // given
        stubFor(delete(schemaVersionsUrl(TOPIC_NAME)).willReturn(methodNotAllowedResponse()));

        // when
        catchException(client).deleteAllSchemaSources(topic);

        // then
        assertThat((Throwable) caughtException()).isInstanceOf(CouldNotRemoveSchemaException.class);
    }

    private UrlMatchingStrategy getSchemaVersion(String topicName, int version) {
        return urlEqualTo("/subjects/" + topicName + "/versions/" + version);
    }

    private UrlMatchingStrategy getSchemaLatestVersionUrl(String topicName) {
        return urlEqualTo("/subjects/" + topicName + "/versions/latest");
    }

    private UrlMatchingStrategy schemaVersionsUrl(String topicName) {
        return urlEqualTo("/subjects/" + topicName + "/versions");
    }

    private UrlMatchingStrategy subjectUrl(String topicName) {
        return urlEqualTo("/subjects/" + topicName);
    }

    private ResponseDefinitionBuilder okResponse() {
        return aResponse().withStatus(200);
    }

    private ResponseDefinitionBuilder badRequestResponse() {
        return aResponse().withStatus(400);
    }

    private ResponseDefinitionBuilder methodNotAllowedResponse() {
        return aResponse().withStatus(405);
    }

    private ResponseDefinitionBuilder notFoundResponse() {
        return aResponse().withStatus(404);
    }

    private ResponseDefinitionBuilder internalErrorResponse() {
        return aResponse().withStatus(500);
    }
}