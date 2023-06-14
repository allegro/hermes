package pl.allegro.tech.hermes.integration.management;

import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integration.IntegrationTest;
import pl.allegro.tech.hermes.test.helper.avro.AvroUserSchemaLoader;

import jakarta.ws.rs.core.Response;

import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.api.TopicWithSchema.topicWithSchema;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.randomTopic;

public class SchemaManagementTest extends IntegrationTest {

    private static final String SCHEMA_V1 = AvroUserSchemaLoader.load().toString();

    private static final String SCHEMA_V2 = AvroUserSchemaLoader.load("/schema/user_v2.avsc").toString();

    @Test
    public void shouldNotSaveSchemaForInvalidTopic() {
        // given && when
        Response response = management.schema().save("wrongGroup.wrongTopic", "{}");

        // then
        assertThat(response).hasStatus(Response.Status.NOT_FOUND);
    }

    @Test
    public void shouldSaveSchemaForExistingTopic() {
        // given
        Topic topic = randomTopic("schemaGroup1", "schemaTopic1").withContentType(AVRO).build();
        operations.buildTopicWithSchema(topicWithSchema(topic, SCHEMA_V1));

        // when
        Response response = management.schema().save(topic.getQualifiedName(), SCHEMA_V2);

        // then
        assertThat(response).hasStatus(Response.Status.CREATED);
    }

    @Test
    public void shouldReturnSchemaForTopic() {
        // given
        Topic topic = randomTopic("schemaGroup2", "schemaTopic2").withContentType(AVRO).build();
        operations.buildTopicWithSchema(topicWithSchema(topic, SCHEMA_V1));

        // when
        Response response = management.schema().get(topic.getQualifiedName());

        // then
        assertThat(response.readEntity(String.class)).isEqualTo(SCHEMA_V1);
    }

    @Test
    public void shouldRespondWithNoContentOnMissingSchema() {
        // when
        Response response = management.schema().get("schemaGroup3.schemaTopic3");

        // then
        assertThat(response).hasStatus(Response.Status.NO_CONTENT);
    }

    @Test
    public void shouldSuccessfullyRemoveSchemaWhenSchemaRemovingIsEnabled() {
        // given
        Topic topic = randomTopic("avroGroup", "avroTopic").withContentType(AVRO).build();
        operations.buildTopicWithSchema(topicWithSchema(topic, SCHEMA_V1));

        // when
        Response response = management.schema().delete(topic.getQualifiedName());

        // then
        assertThat(response).hasStatus(Response.Status.OK);
    }

    @Test
    public void shouldNotSaveInvalidAvroSchema() {
        // given
        Topic topic = randomTopic("avroGroup", "avroTopic").withContentType(AVRO).build();
        operations.buildTopicWithSchema(topicWithSchema(topic, SCHEMA_V1));

        // when
        Response response = management.schema().save(topic.getQualifiedName(), "{");

        // then
        assertThat(response).hasStatus(Response.Status.BAD_REQUEST);
    }

    @Test
    public void shouldReturnBadRequestDueToNoSchemaValidatorForJsonTopic() {
        // given
        Topic topic = operations.buildTopic(randomTopic("someGroup", "jsonTopic").build());

        // when
        Response response = management.schema().save(topic.getQualifiedName(), true, SCHEMA_V1);

        // then
        assertThat(response).hasStatus(Response.Status.BAD_REQUEST);
    }
}
