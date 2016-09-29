package pl.allegro.tech.hermes.consumers.consumer.converter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.consumers.consumer.Message;
import pl.allegro.tech.hermes.schema.CompiledSchema;
import pl.allegro.tech.hermes.schema.SchemaVersion;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;

import java.io.IOException;

import static com.google.common.collect.ImmutableMap.of;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static pl.allegro.tech.hermes.consumers.consumer.Message.message;
import static pl.allegro.tech.hermes.test.helper.builder.TopicBuilder.topic;

@RunWith(MockitoJUnitRunner.class)
public class AvroToJsonMessageConverterTest {

    @Test
    public void shouldConvertToJsonWithoutMetadata() throws IOException {
        // given
        Topic topic = topic("group.topic").build();
        AvroUser avroUser = new AvroUser("Bob", 18, "blue");
        Message source = message()
                .withData(avroUser.asBytes())
                .withSchema(new CompiledSchema<>(avroUser.getSchema(), SchemaVersion.valueOf(0)))
                .withExternalMetadata(of())
                .build();
        AvroToJsonMessageConverter converter = new AvroToJsonMessageConverter();

        // when
        Message target = converter.convert(source, topic);

        // then
        assertThatJson(new String(target.getData())).isEqualTo("{\"name\": \"Bob\", \"age\": 18, \"favoriteColor\": \"blue\"}");
    }

}