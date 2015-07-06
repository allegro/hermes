package pl.allegro.tech.hermes.common.message.converter;

import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DecoderFactory;
import org.junit.Before;
import org.junit.Test;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.StrictAssertions.assertThatThrownBy;

public class JsonToAvroConverterTest {
    private JsonToAvroConverter converter = new JsonToAvroConverter();
    private AvroUser avroUser;

    @Before
    public void setup() throws IOException {
        this.avroUser = new AvroUser();
    }

    @Test
    public void shouldConvertToAvro() throws IOException {
        // given
        String json = "{\"name\": \"Bob\",\"age\": 50,\"favoriteColor\": \"blue\"}";

        // when
        byte[] avro = converter.convert(json.getBytes(), avroUser.getSchema());
        
        // then
        BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(avro, null);
        assertThat(decoder.readString()).isEqualTo("Bob");
        assertThat(decoder.readInt()).isEqualTo(50);
        assertThat(decoder.readString()).isEqualTo("blue");
    }

    @Test
    public void shouldThrowConvertingExceptionWhenJsonDoNotConformToSchema() {
        // given
        String json = "{\"surname\": \"Bob\",\"favoriteCar\": \"ferrari\"}";

        // when & then
        assertThatThrownBy(() ->
                converter.convert(json.getBytes(), avroUser.getSchema())).isInstanceOf(ConvertingException.class);
    }

}
