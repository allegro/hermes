package pl.allegro.tech.hermes.infrastructure.schemarepo;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.UrlMatchingStrategy;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import java.net.URI;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.StrictAssertions.assertThat;

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
        // given & when
        client.registerSchema(SUBJECT, "{}");

        // then
        verify(1, putRequestedFor(registerSchemaUrl()).withHeader("Content-type", equalTo(MediaType.TEXT_PLAIN)).withRequestBody(equalTo("{}")));
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

    private ResponseDefinitionBuilder notFoundResponse() {
        return aResponse().withStatus(404);
    }
}