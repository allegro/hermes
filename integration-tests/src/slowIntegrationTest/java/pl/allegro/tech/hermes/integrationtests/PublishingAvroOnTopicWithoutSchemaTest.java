package pl.allegro.tech.hermes.integrationtests;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.allegro.tech.hermes.api.ErrorDescription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicWithSchema;
import pl.allegro.tech.hermes.integrationtests.client.FrontendTestClient;
import pl.allegro.tech.hermes.integrationtests.setup.HermesFrontendTestApp;
import pl.allegro.tech.hermes.integrationtests.setup.HermesInitHelper;
import pl.allegro.tech.hermes.integrationtests.setup.HermesManagementTestApp;
import pl.allegro.tech.hermes.integrationtests.setup.InfrastructureExtension;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;
import pl.allegro.tech.hermes.test.helper.avro.AvroUserSchemaLoader;
import pl.allegro.tech.hermes.test.helper.util.Ports;

import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.api.ErrorCode.SCHEMA_COULD_NOT_BE_LOADED;
import static pl.allegro.tech.hermes.api.TopicWithSchema.topicWithSchema;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.SCHEMA_REPOSITORY_SERVER_URL;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

public class PublishingAvroOnTopicWithoutSchemaTest {

    @RegisterExtension
    public static InfrastructureExtension infra = new InfrastructureExtension();

    private static final HermesManagementTestApp management = new HermesManagementTestApp(infra.hermesZookeeper(), infra.kafka(), infra.schemaRegistry());
    private static HermesInitHelper initHelper;
    private static FrontendTestClient publisher;
    private static HermesFrontendTestApp frontend;

    private static final WireMockServer emptySchemaRegistryMock = new WireMockServer(Ports.nextAvailable());

    @BeforeAll
    public static void setup() {
        management.start();
        initHelper = new HermesInitHelper(management.getPort());
        emptySchemaRegistryMock.start();

        frontend = new HermesFrontendTestApp(infra.hermesZookeeper(), infra.kafka(), infra.schemaRegistry());
        frontend.withProperty(SCHEMA_REPOSITORY_SERVER_URL, "http://localhost:" + emptySchemaRegistryMock.port());
        frontend.start();

        publisher = new FrontendTestClient(frontend.getPort());

    }

    @AfterAll
    public static void clean() {
        management.stop();
        emptySchemaRegistryMock.stop();
        frontend.stop();
    }

    @Test
    public void shouldReturnServerInternalErrorResponseOnMissingSchema() {
        // given
        TopicWithSchema topicWithSchema = topicWithSchema(topicWithRandomName()
                .withContentType(AVRO)
                .build(), AvroUserSchemaLoader.load().toString());

        Topic topic = initHelper.createTopicWithSchema(topicWithSchema);

        // when
        String message = new AvroUser("Bob", 50, "blue").asJson();
        WebTestClient.ResponseSpec response = publisher.publish(topic.getQualifiedName(), message);

        // then
        response.expectStatus().is5xxServerError();
        Assertions.assertThat(response.expectBody(ErrorDescription.class).returnResult().getResponseBody().getCode()).isEqualTo(SCHEMA_COULD_NOT_BE_LOADED);
    }
}
