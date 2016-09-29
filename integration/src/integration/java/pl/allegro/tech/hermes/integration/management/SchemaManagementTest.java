package pl.allegro.tech.hermes.integration.management;

import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integration.IntegrationTest;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;

import javax.ws.rs.core.Response;
import java.io.IOException;

import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;

public class SchemaManagementTest extends IntegrationTest {

    private static final String EXAMPLE_SCHEMA = "\"string\"";

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
        operations.buildAvroTopic("schemaGroup1", "schemaTopic1");

        // when
        Response response = management.schema().save("schemaGroup1.schemaTopic1", EXAMPLE_SCHEMA);

        // then
        assertThat(response).hasStatus(Response.Status.CREATED);
    }

    @Test
    public void shouldReturnSchemaForTopic() {
        // given
        operations.buildAvroTopic("schemaGroup2", "schemaTopic2");
        management.schema().save("schemaGroup2.schemaTopic2", EXAMPLE_SCHEMA);

        // when
        Response response = management.schema().get("schemaGroup2.schemaTopic2");

        // then
        assertThat(response.readEntity(String.class)).isEqualTo(EXAMPLE_SCHEMA);
    }

    @Test
    public void shouldRespondWithNoContentOnMissingSchema() {
        // given
        operations.buildAvroTopic("schemaGroup3", "schemaTopic3");

        // when
        Response response = management.schema().get("schemaGroup3.schemaTopic3");

        // then
        assertThat(response).hasStatus(Response.Status.NO_CONTENT);
    }

    @Test
    public void shouldReturnMethodNotAcceptableWhenRemovingSchemaIsDisabled() throws IOException {
        // given
        AvroUser avroUser = new AvroUser();
        Topic avroTopic = topic("avroGroup", "avroTopic")
                .withContentType(ContentType.AVRO)
                .build();
        operations.buildTopic(avroTopic);
        operations.saveSchema(avroTopic, avroUser.getSchemaAsString());

        // when
        Response response = management.schema().delete("avroGroup.avroTopic");

        // then
        assertThat(response).hasStatus(Response.Status.NOT_ACCEPTABLE);
    }

    @Test
    public void shouldNotSaveInvalidAvroSchema() throws IOException {
        // given
        AvroUser avroUser = new AvroUser();
        Topic avroTopic = operations.buildTopic(topic("avroGroup", "avroTopic")
                .withContentType(ContentType.AVRO)
                .build()
        );
        operations.saveSchema(avroTopic, avroUser.getSchemaAsString());

        // when
        Response response = management.schema().save("avroGroup.avroTopic", "{");

        // then
        assertThat(response).hasStatus(Response.Status.BAD_REQUEST);
    }
}
