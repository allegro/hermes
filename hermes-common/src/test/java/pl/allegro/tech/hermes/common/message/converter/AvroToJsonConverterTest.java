package pl.allegro.tech.hermes.common.message.converter;

import org.junit.Before;
import org.junit.Test;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;

import java.io.IOException;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;

public class AvroToJsonConverterTest {

    private AvroToJsonConverter avroToJsonConverter;
    private AvroUser avroUser;

    @Before
    public void setup() throws IOException {
        avroUser = new AvroUser();
        avroToJsonConverter = new AvroToJsonConverter(avroUser.getSchema());
    }

    @Test
    public void shouldConvertAvroToJson() throws IOException {
        //when
        byte [] json = avroToJsonConverter.convert(avroUser.create("Bob", 50, "blue"));

        //then
        assertThatJson(new String(json)).isEqualTo("{\"name\": \"Bob\",\"age\": 50,\"favoriteColor\": \"blue\"}");
    }
}