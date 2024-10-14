package pl.allegro.tech.hermes.integrationtests.management;

import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.api.ContentType.JSON;
import static pl.allegro.tech.hermes.api.TopicWithSchema.topicWithSchema;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topicWithRandomName;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.api.TopicWithSchema;
import pl.allegro.tech.hermes.integrationtests.setup.HermesExtension;
import pl.allegro.tech.hermes.test.helper.avro.AvroUserSchemaLoader;

public class SchemaManagementTest {

  @RegisterExtension public static final HermesExtension hermes = new HermesExtension();

  private static final String SCHEMA_V1 = AvroUserSchemaLoader.load().toString();

  private static final String SCHEMA_V2 =
      AvroUserSchemaLoader.load("/schema/user_v2.avsc").toString();

  @Test
  public void shouldNotSaveSchemaForInvalidTopic() {
    // given && when
    Topic topic = topicWithRandomName().build();
    WebTestClient.ResponseSpec response = hermes.api().saveSchema(topic.getQualifiedName(), "{}");

    // then
    response.expectStatus().isNotFound();
  }

  @Test
  public void shouldSaveSchemaForExistingTopic() {
    // given
    TopicWithSchema topicWithSchema =
        topicWithSchema(topicWithRandomName().withContentType(AVRO).build(), SCHEMA_V1);

    Topic topic = hermes.initHelper().createTopicWithSchema(topicWithSchema);

    // when
    WebTestClient.ResponseSpec response =
        hermes.api().saveSchema(topic.getQualifiedName(), SCHEMA_V2);

    // then
    response.expectStatus().isCreated();
  }

  @Test
  public void shouldReturnSchemaForTopic() {
    // given
    TopicWithSchema topicWithSchema =
        topicWithSchema(topicWithRandomName().withContentType(AVRO).build(), SCHEMA_V1);

    Topic topic = hermes.initHelper().createTopicWithSchema(topicWithSchema);

    // when
    WebTestClient.ResponseSpec response = hermes.api().getSchema(topic.getQualifiedName());

    // then
    response.expectStatus().isOk().expectBody(String.class).isEqualTo(SCHEMA_V1);
  }

  @Test
  public void shouldRespondWithNoContentOnMissingSchema() {
    // when
    Topic topic = topicWithRandomName().build();

    WebTestClient.ResponseSpec response = hermes.api().getSchema(topic.getQualifiedName());

    // then
    response.expectStatus().isNoContent();
  }

  @Test
  public void shouldSuccessfullyRemoveSchemaWhenSchemaRemovingIsEnabled() {
    // given
    TopicWithSchema topicWithSchema =
        topicWithSchema(topicWithRandomName().withContentType(AVRO).build(), SCHEMA_V1);

    Topic topic = hermes.initHelper().createTopicWithSchema(topicWithSchema);

    // when
    WebTestClient.ResponseSpec response = hermes.api().deleteSchema(topic.getQualifiedName());

    // then
    response.expectStatus().isOk();
  }

  @Test
  public void shouldNotSaveInvalidAvroSchema() {
    // given
    TopicWithSchema topicWithSchema =
        topicWithSchema(topicWithRandomName().withContentType(AVRO).build(), SCHEMA_V1);

    Topic topic = hermes.initHelper().createTopicWithSchema(topicWithSchema);

    // when
    WebTestClient.ResponseSpec response = hermes.api().saveSchema(topic.getQualifiedName(), "{");

    // then
    response.expectStatus().isBadRequest();
  }

  @Test
  public void shouldReturnBadRequestDueToNoSchemaValidatorForJsonTopic() {
    // given
    Topic topic = topicWithRandomName().withContentType(JSON).build();

    hermes.initHelper().createTopic(topic);

    // when
    WebTestClient.ResponseSpec response =
        hermes.api().saveSchema(topic.getQualifiedName(), true, SCHEMA_V1);

    // then
    response.expectStatus().isBadRequest();
  }
}
