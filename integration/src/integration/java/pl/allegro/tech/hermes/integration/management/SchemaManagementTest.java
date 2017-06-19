package pl.allegro.tech.hermes.integration.management;

import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integration.IntegrationTest;
import pl.allegro.tech.hermes.test.helper.avro.AvroUserSchemaLoader;

import javax.ws.rs.core.Response;
import java.io.IOException;

import static pl.allegro.tech.hermes.api.ContentType.AVRO;
import static pl.allegro.tech.hermes.api.TopicWithSchema.topicWithSchema;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;

public class SchemaManagementTest extends IntegrationTest {

    private static final String EXAMPLE_SCHEMA = "\"string\"";

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
        Topic topic = topic("schemaGroup1", "schemaTopic1").withContentType(AVRO).build();
        operations.buildTopicWithSchema(topicWithSchema(topic, SCHEMA_V1));

        // when
        Response response = management.schema().save("schemaGroup1.schemaTopic1", SCHEMA_V2);

        // then
        assertThat(response).hasStatus(Response.Status.CREATED);
    }

    @Test
    public void shouldReturnSchemaForTopic() {
        // given
        Topic topic = topic("schemaGroup2", "schemaTopic2").withContentType(AVRO).build();
        operations.buildTopicWithSchema(topicWithSchema(topic, EXAMPLE_SCHEMA));

        // when
        Response response = management.schema().get("schemaGroup2.schemaTopic2");

        // then
        assertThat(response.readEntity(String.class)).isEqualTo(EXAMPLE_SCHEMA);
    }

    @Test
    public void shouldRespondWithNoContentOnMissingSchema() {
        // when
        Response response = management.schema().get("schemaGroup3.schemaTopic3");

        // then
        assertThat(response).hasStatus(Response.Status.NO_CONTENT);
    }

    @Test
    public void shouldReturnMethodNotAcceptableWhenRemovingSchemaIsDisabled() throws IOException {
        // given
        Topic topic = topic("avroGroup", "avroTopic").withContentType(AVRO).build();
        operations.buildTopicWithSchema(topicWithSchema(topic, EXAMPLE_SCHEMA));

        // when
        Response response = management.schema().delete("avroGroup.avroTopic");

        // then
        assertThat(response).hasStatus(Response.Status.NOT_ACCEPTABLE);
    }

    @Test
    public void shouldNotSaveInvalidAvroSchema() throws IOException {
        // given
        Topic topic = topic("avroGroup", "avroTopic").withContentType(AVRO).build();
        operations.buildTopicWithSchema(topicWithSchema(topic, EXAMPLE_SCHEMA));

        // when
        Response response = management.schema().save("avroGroup.avroTopic", "{");

        // then
        assertThat(response).hasStatus(Response.Status.BAD_REQUEST);
    }

    @Test
    public void shouldReturnBadRequestDueToNoSchemaValidatorForJsonTopic() {
        // given
        operations.buildTopic("someGroup", "jsonTopic");

        // when
        Response response = management.schema().save("someGroup.jsonTopic", true, EXAMPLE_SCHEMA);

        // then
        assertThat(response).hasStatus(Response.Status.BAD_REQUEST);
    }
}
