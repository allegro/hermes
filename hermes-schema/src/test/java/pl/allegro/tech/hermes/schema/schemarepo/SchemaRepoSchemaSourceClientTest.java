package pl.allegro.tech.hermes.schema.schemarepo;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.UrlMatchingStrategy;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import pl.allegro.tech.hermes.api.SchemaSource;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.schema.InvalidSchemaException;
import pl.allegro.tech.hermes.schema.SchemaRepositoryServerException;
import pl.allegro.tech.hermes.schema.SchemaSourceClient;
import pl.allegro.tech.hermes.schema.SchemaVersion;
import pl.allegro.tech.hermes.test.helper.util.Ports;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.assertj.core.api.Assertions.assertThat;

public class SchemaRepoSchemaSourceClientTest {

    private static final String ROOT_DIR = "/schema-repo/";
    private static final int PORT = Ports.nextAvailable();

    private static final String TOPIC_NAME = "group.topic";
    private static final TopicName topic = TopicName.fromQualifiedName(TOPIC_NAME);
    private static final SchemaSource schemaSource = SchemaSource.valueOf("{}");

    private static final SchemaSourceClient client = new SchemaRepoSchemaSourceClient(
            ClientBuilder.newClient(),
            URI.create("http://localhost:" + PORT + ROOT_DIR)
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
    public void shouldRegisterSubjectAndSchemaSource() {
        // given
        stubFor(get(subjectUrl(TOPIC_NAME)).willReturn(notFoundResponse()));
        stubFor(put(subjectUrl(TOPIC_NAME)).willReturn(okResponse()));
        stubFor(put(registerSchemaUrl(TOPIC_NAME)).willReturn(okResponse()));

        // when
        client.registerSchemaSource(topic, schemaSource);

        // then
        verify(1, getRequestedFor(subjectUrl(TOPIC_NAME)));
        verify(1, putRequestedFor(subjectUrl(TOPIC_NAME)));
        verify(1, putRequestedFor(registerSchemaUrl(TOPIC_NAME))
                .withHeader("Content-type", equalTo(MediaType.TEXT_PLAIN))
                .withRequestBody(equalTo(schemaSource.value())));
    }

    @Test
    public void shouldRegisterSchemaSourceWhenSubjectIsAlreadyRegistered() {
        // given
        stubFor(get(subjectUrl(TOPIC_NAME)).willReturn(okResponse()));
        stubFor(put(registerSchemaUrl(TOPIC_NAME)).willReturn(okResponse()));

        // when
        client.registerSchemaSource(topic, schemaSource);

        // then
        verify(1, getRequestedFor(subjectUrl(TOPIC_NAME)));
        verify(0, putRequestedFor(subjectUrl(TOPIC_NAME)));
        verify(1, putRequestedFor(registerSchemaUrl(TOPIC_NAME))
                .withHeader("Content-type", equalTo(MediaType.TEXT_PLAIN))
                .withRequestBody(equalTo(schemaSource.value())));
    }

    @Test
    public void shouldThrowExceptionForUnsuccessfulSubjectRegistration() {
        // given
        stubFor(get(subjectUrl(TOPIC_NAME)).willReturn(notFoundResponse()));
        stubFor(put(subjectUrl(TOPIC_NAME)).willReturn(serverErrorResponse()));

        // when
        catchException(client).registerSchemaSource(topic, schemaSource);

        // then
        assertThat((Throwable) caughtException()).isInstanceOf(SchemaRepositoryServerException.class);
    }

    @Test
    public void shouldReturnEmptyOptionalIfLatestSchemaDoesNotExist() {
        // given
        stubFor(get(latestSchemaUrl(TOPIC_NAME)).willReturn(notFoundResponse()));

        // when
        Optional<SchemaSource> schema = client.getLatestSchemaSource(topic);

        // then
        assertThat(schema.isPresent()).isFalse();
    }

    @Test
    public void shouldReturnLatestSchemaIfExists() {
        // given
        stubFor(get(latestSchemaUrl(TOPIC_NAME)).willReturn(okResponse().withBody("0\t{}")));

        // when
        Optional<SchemaSource> schema = client.getLatestSchemaSource(topic);

        // then
        assertThat(schema.get().value()).isEqualTo("{}");
    }


    @Test
    public void shouldThrowExceptionForInvalidSchemaRegistration() {
        // given
        stubFor(get(subjectUrl(TOPIC_NAME)).willReturn(okResponse()));
        stubFor(put(registerSchemaUrl(TOPIC_NAME)).willReturn(forbiddenResponse("error")));

        // when
        catchException(client).registerSchemaSource(topic, schemaSource);

        // then
        assertThat((Throwable) caughtException())
                .isInstanceOf(InvalidSchemaException.class)
                .hasMessageContaining("error");
    }

    @Test
    public void shouldThrowSchemaRepoExceptionForSchemaRegistration() {
        // given
        stubFor(get(subjectUrl(TOPIC_NAME)).willReturn(okResponse()));
        stubFor(put(registerSchemaUrl(TOPIC_NAME)).willReturn(serverErrorResponse()));

        // when
        catchException(client).registerSchemaSource(topic, schemaSource);

        // then
        assertThat((Throwable) caughtException()).isInstanceOf(SchemaRepositoryServerException.class);
    }

    @Test
    public void shouldReturnSchemaVersions() {
        // given
        wireMock.stubFor(get(allSchemasUrl(TOPIC_NAME)).willReturn(
                okResponse().withBodyFile("all-schemas-response.json").withHeader("Content-Type", "application/json"))
        );

        // when
        List<SchemaVersion> versions = client.getVersions(topic);

        // then
        Assertions.assertThat(versions).containsExactly(
                SchemaVersion.valueOf(2),
                SchemaVersion.valueOf(1),
                SchemaVersion.valueOf(0)
        );
    }

    @Test
    public void shouldReturnEmptySchemaVersionsIfNoSchemasAreRegistered() {
        // given
        wireMock.stubFor(get(allSchemasUrl(TOPIC_NAME)).willReturn(
                okResponse().withBody("").withHeader("Content-Type", "application/json"))
        );

        // when
        List<SchemaVersion> versions = client.getVersions(topic);

        // then
        Assertions.assertThat(versions).isEmpty();
    }

    @Test
    public void shouldReturnEmptySchemaVersionsIfSubjectDoesntExist() {
        // given
        wireMock.stubFor(get(allSchemasUrl(TOPIC_NAME)).willReturn(notFoundResponse()));

        // when
        List<SchemaVersion> versions = client.getVersions(topic);

        // then
        Assertions.assertThat(versions).isEmpty();
    }

    private UrlMatchingStrategy subjectUrl(String name) {
        return urlEqualTo(ROOT_DIR + name);
    }

    private UrlMatchingStrategy latestSchemaUrl(String name) {
        return urlEqualTo(ROOT_DIR + name + "/latest");
    }

    private UrlMatchingStrategy registerSchemaUrl(String name) {
        return urlEqualTo(ROOT_DIR + name + "/register");
    }

    private UrlMatchingStrategy allSchemasUrl(String name) {
        return urlEqualTo(ROOT_DIR + name + "/all");
    }

    private ResponseDefinitionBuilder okResponse() {
        return aResponse().withStatus(200);
    }

    private ResponseDefinitionBuilder serverErrorResponse() {
        return aResponse().withStatus(500);
    }

    private ResponseDefinitionBuilder notFoundResponse() {
        return aResponse().withStatus(404);
    }

    private ResponseDefinitionBuilder forbiddenResponse(String body) {
        return aResponse().withStatus(403).withBody(body);
    }
}