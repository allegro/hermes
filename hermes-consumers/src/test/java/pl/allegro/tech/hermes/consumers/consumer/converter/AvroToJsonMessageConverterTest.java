package pl.allegro.tech.hermes.consumers.consumer.converter;

import org.apache.avro.Schema;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.consumers.consumer.converter.schema.AvroSchemaRepositoryMetadataAware;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;

import java.io.IOException;

import static com.google.common.collect.ImmutableMap.of;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.mockito.Mockito.when;
import static pl.allegro.tech.hermes.consumers.consumer.Message.message;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;

@RunWith(MockitoJUnitRunner.class)
public class AvroToJsonMessageConverterTest {

    @Mock
    private SchemaRepository<Schema> avroSchemaRepository;

    @Test
    public void shouldConvertToJsonWithoutMetadata() throws IOException {
        // given
        Topic topic = topic("group.topic").build();
        AvroUser avroUser = new AvroUser("Bob", 18, "blue");
        Message source = message().withData(avroUser.asBytes()).withExternalMetadata(of()).build();
        when(avroSchemaRepository.getSchema(topic)).thenReturn(avroUser.getSchema());
        AvroToJsonMessageConverter converter = new AvroToJsonMessageConverter(new AvroSchemaRepositoryMetadataAware(avroSchemaRepository));

        // when
        Message target = converter.convert(source, topic);

        // then
        assertThatJson(new String(target.getData())).isEqualTo("{\"name\": \"Bob\", \"age\": 18, \"favoriteColor\": \"blue\"}");
    }

}