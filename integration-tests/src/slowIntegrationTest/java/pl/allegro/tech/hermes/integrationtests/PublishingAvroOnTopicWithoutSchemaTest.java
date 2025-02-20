package pl.allegro.tech.hermes.integrationtests;

import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.api.ErrorCode.SCHEMA_COULD_NOT_BE_LOADED;
import static pl.allegro.tech.hermes.api.TopicWithSchema.topicWithSchema;
import static pl.allegro.tech.hermes.frontend.FrontendConfigurationProperties.SCHEMA_REPOSITORY_SERVER_URL;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.Options;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.allegro.tech.hermes.api.ErrorDescription;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicWithSchema;
import pl.allegro.tech.hermes.integrationtests.setup.HermesFrontendTestApp;
import pl.allegro.tech.hermes.integrationtests.setup.HermesManagementExtension;
import pl.allegro.tech.hermes.integrationtests.setup.InfrastructureExtension;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;
import pl.allegro.tech.hermes.test.helper.avro.AvroUserSchemaLoader;
import pl.allegro.tech.hermes.test.helper.client.integration.FrontendTestClient;

public class PublishingAvroOnTopicWithoutSchemaTest {

  @Order(0)
  @RegisterExtension
  public static InfrastructureExtension infra = new InfrastructureExtension();

  @Order(1)
  @RegisterExtension
  public static HermesManagementExtension management = new HermesManagementExtension(infra);

  private static FrontendTestClient publisher;
  private static HermesFrontendTestApp frontend;

  private static final WireMockServer emptySchemaRegistryMock =
      new WireMockServer(Options.DYNAMIC_PORT);

  @BeforeAll
  public static void setup() {
    emptySchemaRegistryMock.start();

    frontend =
        new HermesFrontendTestApp(infra.hermesZookeeper(), infra.kafka(), infra.schemaRegistry());
    frontend.withProperty(
        SCHEMA_REPOSITORY_SERVER_URL, "http://localhost:" + emptySchemaRegistryMock.port());
    frontend.start();

    publisher = new FrontendTestClient(frontend.getPort());
  }

  @AfterAll
  public static void clean() {
    emptySchemaRegistryMock.stop();
    frontend.stop();
  }

  @Test
  public void shouldReturnServerInternalErrorResponseOnMissingSchema() {
    // given
    TopicWithSchema topicWithSchema =
        topicWithSchema(
            topicWithRandomName().withContentType(AVRO).build(),
            AvroUserSchemaLoader.load().toString());

    Topic topic = management.initHelper().createTopicWithSchema(topicWithSchema);

    // when
    String message = new AvroUser("Bob", 50, "blue").asJson();
    // ensure topic is created
    publisher.publishUntilStatus(topic.getQualifiedName(), message, 500);
    WebTestClient.ResponseSpec response = publisher.publish(topic.getQualifiedName(), message);

    // then
    response.expectStatus().isEqualTo(500);
    Assertions.assertThat(
            response.expectBody(ErrorDescription.class).returnResult().getResponseBody().getCode())
        .isEqualTo(SCHEMA_COULD_NOT_BE_LOADED);
  }
}
