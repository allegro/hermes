package pl.allegro.tech.hermes.common.message.wrapper;

import org.testng.annotations.Test;
import pl.allegro.tech.hermes.schema.SchemaId;
import pl.allegro.tech.hermes.test.helper.avro.AvroUser;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class SchemaAwareSerDeTest {
    static final AvroUser avro = new AvroUser("bob", 10, "red");

    @Test
    public void shouldSerialize() throws IOException {
        // given
        SchemaId id = SchemaId.valueOf(8);

        // when
        byte[] serialized = SchemaAwareSerDe.serialize(id, avro.asBytes());

        // then
        assertThat(serialized).startsWith((byte) 0);
    }

    @Test
    public void shouldDeserialize() throws IOException {
        // given
        byte[] serialized = SchemaAwareSerDe.serialize(SchemaId.valueOf(8), avro.asBytes());

        // when
        SchemaAwarePayload deserialized = SchemaAwareSerDe.deserialize(serialized);

        // then
        assertThat(deserialized.getSchemaId().value()).isEqualTo(8);
        assertThat(deserialized.getPayload()).isEqualTo(avro.asBytes());
    }

    @Test(expectedExceptions = { DeserializationException.class })
    public void shouldThrowExceptionWhenDeserializingWithoutMagicByte() throws IOException {
        // when
        SchemaAwareSerDe.deserialize(new byte[]{1, 2, 3});

        // then exception is thrown
    }
}
