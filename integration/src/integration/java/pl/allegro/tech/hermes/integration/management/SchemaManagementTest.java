package pl.allegro.tech.hermes.integration.management;

import org.testng.annotations.Test;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.integration.IntegrationTest;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;

import javax.ws.rs.core.Response;
import java.io.IOException;

import static pl.allegro.tech.hermes.api.Topic.Builder.topic;
import static pl.allegro.tech.hermes.integration.test.HermesAssertions.assertThat;

public class SchemaManagementTest extends IntegrationTest {

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
        operations.buildTopic("schemaGroup", "schemaTopic");

        // when
        Response response = management.schema().save("schemaGroup.schemaTopic", "{}");

        // then
        assertThat(response).hasStatus(Response.Status.CREATED);
    }

    @Test
    public void shouldReturnSchemaForTopic() {
        // given
        operations.buildTopic("schemaGroup", "schemaTopic");
        management.schema().save("schemaGroup.schemaTopic", "{}");

        // when
        Response response = management.schema().get("schemaGroup.schemaTopic");

        // then
        assertThat(response.readEntity(String.class)).isEqualTo("{}");
    }

    @Test
    public void shouldRespondWithNoContentOnMissingSchema() {
        // given
        operations.buildTopic("schemaGroup", "schemaTopic");

        // when
        Response response = management.schema().get("schemaGroup.schemaTopic");

        // then
        assertThat(response).hasStatus(Response.Status.NO_CONTENT);
    }

    @Test
    public void shouldRemoveJsonSchema() {
        // given
        operations.buildTopic("schemaGroup", "schemaTopic");
        management.schema().save("schemaGroup.schemaTopic", "{}");

        // when
        management.schema().delete("schemaGroup.schemaTopic");

        // then
        Response getResponse = management.schema().get("schemaGroup.schemaTopic");
        assertThat(getResponse).hasStatus(Response.Status.NO_CONTENT);
    }

    @Test
    public void shouldNotSaveInvalidJsonSchema() throws IOException {
        // given
        operations.buildTopic("jsonGroup", "jsonTopic");

        // when
        Response response = management.schema().save("jsonGroup.jsonTopic", "{");

        // then
        assertThat(response).hasStatus(Response.Status.BAD_REQUEST);
    }

    @Test
    public void shouldNotRemoveAvroSchema() throws IOException {
        // given
        AvroUser avroUser = new AvroUser();
        Topic avroTopic = topic().withName("avroGroup", "avroTopic")
                .withContentType(ContentType.AVRO)
                .withMessageSchema(avroUser.getSchema().toString())
                .build();
        operations.buildTopic(avroTopic);

        // when
        Response response = management.schema().delete("avroGroup.avroTopic");

        // then
        assertThat(response).hasStatus(Response.Status.BAD_REQUEST);
    }

    @Test
    public void shouldNotSaveInvalidAvroSchema() throws IOException {
        // given
        AvroUser avroUser = new AvroUser();
        Topic avroTopic = topic().withName("avroGroup", "avroTopic")
                .withContentType(ContentType.AVRO)
                .withMessageSchema(avroUser.getSchema().toString())
                .build();
        operations.buildTopic(avroTopic);

        // when
        Response response = management.schema().save("avroGroup.avroTopic", "{");

        // then
        assertThat(response).hasStatus(Response.Status.BAD_REQUEST);
    }
}
