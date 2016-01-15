package pl.allegro.tech.hermes.test.helper.avro;

import org.apache.avro.Schema;

import java.io.IOException;
import java.io.UncheckedIOException;

public class AvroUserSchemaLoader {

    public static Schema load() {
        try {
            return new Schema.Parser().parse(AvroUserSchemaLoader.class.getResourceAsStream("/schema/user.avsc"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
