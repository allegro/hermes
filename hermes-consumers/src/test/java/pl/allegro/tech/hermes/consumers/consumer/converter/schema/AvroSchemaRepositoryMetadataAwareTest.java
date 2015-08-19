package pl.allegro.tech.hermes.consumers.consumer.converter.schema;

import org.apache.avro.Schema;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;

import java.io.IOException;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.mockito.Mockito.when;
import static pl.allegro.tech.hermes.common.message.converter.AvroToJsonConverter.convert;

@RunWith(MockitoJUnitRunner.class)
public class AvroSchemaRepositoryMetadataAwareTest {

    @Mock
    private SchemaRepository<Schema> avroSchemaRepository;

    private AvroSchemaRepositoryMetadataAware avroSchemaRepositoryMetadataAware;

    @Before
    public void init() {
        avroSchemaRepositoryMetadataAware = new AvroSchemaRepositoryMetadataAware(avroSchemaRepository, 10, 10);
    }

    @Test
    public void shouldGetSchemaWithoutMetadata() throws IOException {
        // given
        Topic topic = Topic.Builder.topic().withName("group.topic").build();
        AvroUser avroUser = new AvroUser();
        when(avroSchemaRepository.getSchema(topic)).thenReturn(avroUser.getSchema());

        // when
        Schema schemaWithoutMetadata = avroSchemaRepositoryMetadataAware.getSchemaWithoutMetadata(topic);

        // then
        String jsonUser = new String(convert(avroUser.create("Bob", 17, "blue"), schemaWithoutMetadata));
        assertThatJson(jsonUser).isEqualTo("{\"name\": \"Bob\", \"age\": 17, \"favoriteColor\": \"blue\"}");
    }

}