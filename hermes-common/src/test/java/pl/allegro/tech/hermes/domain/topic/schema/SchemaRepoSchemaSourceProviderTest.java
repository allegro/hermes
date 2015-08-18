package pl.allegro.tech.hermes.domain.topic.schema;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.UrlMatchingStrategy;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;
import pl.allegro.tech.hermes.api.SchemaSource;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.infrastructure.schema.repo.SchemaRepoClientFactory;

import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static pl.allegro.tech.hermes.api.Topic.Builder.topic;

public class SchemaRepoSchemaSourceProviderTest {

    private static final String ROOT_DIR = "/schema-repo/";

    private static final SchemaRepoSchemaSourceProvider sourceProvider = new SchemaRepoSchemaSourceProvider(new SchemaRepoClientFactory(new ConfigFactory()).provide());

    private static final Topic topic = topic().withName("someGroup.someTopic").build();

    @Rule
    public final WireMockRule wireMockRule = new WireMockRule(wireMockConfig().port(2876));

    @Test
    public void shouldReturnEmptyOptionalOnMissingSchema() {
        // given
        wireMockRule.stubFor(get(topicLatestSchema()).willReturn(notFoundResponse()));

        // when
        Optional<SchemaSource> source = sourceProvider.get(topic);

        // then
        wireMockRule.verify(1, getRequestedFor(topicLatestSchema()));
        assertThat(source).isEmpty();
    }

    @Test
    public void shouldReturnSchemaSourceWhenAvailable() {
        // given
        wireMockRule.stubFor(get(topicLatestSchema()).willReturn(okResponse().withBody("0\tsomeSchema")));

        // when
        Optional<SchemaSource> source = sourceProvider.get(topic);

        // then
        wireMockRule.verify(1, getRequestedFor(topicLatestSchema()));
        assertThat(source).contains(SchemaSource.valueOf("someSchema"));
    }

    private UrlMatchingStrategy topicLatestSchema() {
        return urlEqualTo(ROOT_DIR + topic.getQualifiedName() + "/latest");
    }

    private ResponseDefinitionBuilder okResponse() {
        return aResponse().withStatus(200);
    }

    private ResponseDefinitionBuilder notFoundResponse() {
        return aResponse().withStatus(404);
    }
}