package pl.allegro.tech.hermes.infrastructure.schema.repo;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.UrlMatchingStrategy;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.testng.annotations.BeforeTest;
import pl.allegro.tech.hermes.common.exception.InvalidSchemaException;
import pl.allegro.tech.hermes.common.exception.SchemaRepoException;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaVersion;
import pl.allegro.tech.hermes.test.helper.util.Ports;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.assertj.core.api.Assertions.assertThat;

public class JerseySchemaRepoClientTest {

    private static final String ROOT_DIR = "/schema-repo/";
    private static final int PORT = Ports.nextAvailable();

    private static final SchemaRepoClient client = new JerseySchemaRepoClient(
            ClientBuilder.newClient(),
            URI.create("http://localhost:" + PORT + ROOT_DIR)
    );

    @ClassRule
    public static final WireMockRule wireMock = new WireMockRule(
            wireMockConfig().port(PORT).usingFilesUnderClasspath("schema-repo-stub")
    );

    @BeforeTest
    public void initialize() {
        wireMock.resetRequests();
        wireMock.resetMappings();
        wireMock.resetScenarios();
    }

    @Test
    public void shouldCheckIfSubjectIsRegistered() {
        // given
        wireMock.stubFor(get(subjectUrl("notRegistered.subject")).willReturn(notFoundResponse()));

        // when
        boolean exists = client.isSubjectRegistered("notRegistered.subject");

        // then
        wireMock.verify(1, getRequestedFor(subjectUrl("notRegistered.subject")));
        assertThat(exists).isFalse();
    }

    @Test
    public void shouldRegisterSubject() {
        // given
        wireMock.stubFor(put(subjectUrl("register.subject")).willReturn(okResponse()));

        // when
        client.registerSubject("register.subject");

        // then
        verify(putRequestedFor(subjectUrl("register.subject"))
                .withHeader("Content-type", equalTo(MediaType.APPLICATION_FORM_URLENCODED)));
    }

    @Test
    public void shouldThrowExceptionForUnsuccessfulSubjectRegistration() {
        // given
        wireMock.stubFor(put(subjectUrl("failed.subject")).willReturn(serverErrorResponse()));

        // when
        catchException(client).registerSubject("failed.subject");

        // then
        assertThat((Throwable) caughtException()).isInstanceOf(SchemaRepoException.class);
    }

    @Test
    public void shouldReturnEmptyOptionalIfLatestSchemaDoesNotExist() {
        // given
        wireMock.stubFor(get(latestSchemaUrl("nolatest.subject")).willReturn(notFoundResponse()));

        // when
        Optional<String> schema = client.getLatestSchema("nolatest.subject");

        // then
        assertThat(schema.isPresent()).isFalse();
    }

    @Test
    public void shouldReturnLatestSchemaIfExists() {
        // given
        wireMock.stubFor(get(latestSchemaUrl("latest.subject")).willReturn(okResponse().withBody("0\t{}")));

        // when
        Optional<String> schema = client.getLatestSchema("latest.subject");

        // then
        assertThat(schema.get()).isEqualTo("{}");
    }

    @Test
    public void shouldRegisterSchema() {
        // given
        wireMock.stubFor(put(registerSchemaUrl("registerSchema.subject")).willReturn(okResponse()));

        // when
        client.registerSchema("registerSchema.subject", "{}");

        // then
        verify(1, putRequestedFor(registerSchemaUrl("registerSchema.subject"))
                .withHeader("Content-type", equalTo(MediaType.TEXT_PLAIN))
                .withRequestBody(equalTo("{}"))
        );
    }

    @Test
    public void shouldThrowExceptionForInvalidSchemaRegistration() {
        // given
        wireMock.stubFor(put(registerSchemaUrl("invalidSchema.subject")).willReturn(forbiddenResponse("error")));

        // when
        catchException(client).registerSchema("invalidSchema.subject", "{}");

        // then
        assertThat((Throwable) caughtException())
                .isInstanceOf(InvalidSchemaException.class)
                .hasMessageContaining("error");
    }

    @Test
    public void shouldThrowSchemaRepoExceptionForSchemaRegistration() {
        // given
        wireMock.stubFor(put(registerSchemaUrl("repoException.subject")).willReturn(serverErrorResponse()));

        // when
        catchException(client).registerSchema("repoException.subject", "{}");

        // then
        assertThat((Throwable) caughtException()).isInstanceOf(SchemaRepoException.class);
    }

    @Test
    public void shouldReturnSchemaVersions() {
        // given
        wireMock.stubFor(get(allSchemasUrl("allSchemas.subject")).willReturn(
                okResponse().withBodyFile("all-schemas-response.json").withHeader("Content-Type", "application/json"))
        );

        // when
        List<SchemaVersion> versions = client.getSchemaVersions("allSchemas.subject");

        // then
        assertThat(versions).containsExactly(
                SchemaVersion.valueOf(2),
                SchemaVersion.valueOf(1),
                SchemaVersion.valueOf(0)
        );
    }

    @Test
    public void shouldReturnEmptySchemaVersionsIfNoSchemasAreRegistered() {
        // given
        wireMock.stubFor(get(allSchemasUrl("noSchema.subject")).willReturn(
                okResponse().withBody("").withHeader("Content-Type", "application/json"))
        );

        // when
        List<SchemaVersion> versions = client.getSchemaVersions("noSchema.subject");

        // then
        assertThat(versions).isEmpty();
    }

    @Test
    public void shouldReturnEmptySchemaVersionsIfSubjectDoesntExist() {
        // given
        wireMock.stubFor(get(allSchemasUrl("noSubject.subject")).willReturn(notFoundResponse()));

        // when
        List<SchemaVersion> versions = client.getSchemaVersions("noSubject.subject");

        // then
        assertThat(versions).isEmpty();
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