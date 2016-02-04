package pl.allegro.tech.hermes.infrastructure.schema.repo;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.UrlMatchingStrategy;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;
import pl.allegro.tech.hermes.common.exception.InvalidSchemaException;
import pl.allegro.tech.hermes.common.exception.SchemaRepoException;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.googlecode.catchexception.CatchException.catchException;
import static com.googlecode.catchexception.CatchException.caughtException;
import static org.assertj.core.api.Assertions.assertThat;

public class JerseySchemaRepoClientTest {

    private static final String ROOT_DIR = "/schema-repo/";
    private static final String SUBJECT = "some.subject";

    private static final SchemaRepoClient client = new JerseySchemaRepoClient(ClientBuilder.newClient(), URI.create("http://localhost:2876/schema-repo/"));

    @Rule
    public final WireMockRule wireMockRule = new WireMockRule(wireMockConfig().port(2876));

    @Test
    public void shouldCheckIfSubjectIsRegistered() {
        // given
        wireMockRule.stubFor(get(subjectUrl()).willReturn(notFoundResponse()));

        // when
        boolean exists = client.isSubjectRegistered(SUBJECT);

        // then
        wireMockRule.verify(1, getRequestedFor(subjectUrl()));
        assertThat(exists).isFalse();
    }

    @Test
    public void shouldRegisterSubject() {
        // given
        wireMockRule.stubFor(put(subjectUrl()).willReturn(okResponse()));

        // when
        client.registerSubject(SUBJECT);

        // then
        verify(1, putRequestedFor(subjectUrl()).withHeader("Content-type", equalTo(MediaType.APPLICATION_FORM_URLENCODED)));
    }

    @Test
    public void shouldThrowExceptionForUnsuccessfulSubjectRegistration() {
        // given
        wireMockRule.stubFor(put(subjectUrl()).willReturn(serverErrorResponse()));

        // when
        catchException(client).registerSubject(SUBJECT);

        // then
        assertThat((Throwable) caughtException()).isInstanceOf(SchemaRepoException.class);
    }

    @Test
    public void shouldReturnEmptyOptionalIfLatestSchemaDoesNotExist() {
        // given
        wireMockRule.stubFor(get(latestSchemaUrl()).willReturn(notFoundResponse()));

        // when
        Optional<String> schema = client.getLatestSchema(SUBJECT);

        // then
        assertThat(schema.isPresent()).isFalse();
    }

    @Test
    public void shouldReturnLatestSchemaIfExists() {
        // given
        wireMockRule.stubFor(get(latestSchemaUrl()).willReturn(okResponse().withBody("0\t{}")));

        // when
        Optional<String> schema = client.getLatestSchema(SUBJECT);

        // then
        assertThat(schema.get()).isEqualTo("{}");
    }

    @Test
    public void shouldRegisterSchema() {
        // given
        wireMockRule.stubFor(put(registerSchemaUrl()).willReturn(okResponse()));

        // when
        client.registerSchema(SUBJECT, "{}");

        // then
        verify(1, putRequestedFor(registerSchemaUrl()).withHeader("Content-type", equalTo(MediaType.TEXT_PLAIN)).withRequestBody(equalTo("{}")));
    }

    @Test
    public void shouldThrowExceptionForInvalidSchemaRegistration() {
        // given
        wireMockRule.stubFor(put(registerSchemaUrl()).willReturn(badRequestResponse()));

        // when
        catchException(client).registerSchema(SUBJECT, "{}");

        // then
        assertThat((Throwable) caughtException()).isInstanceOf(InvalidSchemaException.class);
    }

    @Test
    public void shouldThrowSchemaRepoExceptionForSchemaRegistration() {
        // given
        wireMockRule.stubFor(put(registerSchemaUrl()).willReturn(serverErrorResponse()));

        // when
        catchException(client).registerSchema(SUBJECT, "{}");

        // then
        assertThat((Throwable) caughtException()).isInstanceOf(SchemaRepoException.class);
    }

    private UrlMatchingStrategy subjectUrl() {
        return urlEqualTo(ROOT_DIR + SUBJECT);
    }

    private UrlMatchingStrategy latestSchemaUrl() {
        return urlEqualTo(ROOT_DIR + SUBJECT + "/latest");
    }

    private UrlMatchingStrategy registerSchemaUrl() {
        return urlEqualTo(ROOT_DIR + SUBJECT + "/register");
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

    private ResponseDefinitionBuilder badRequestResponse() {
        return aResponse().withStatus(400);
    }
}