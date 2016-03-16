package pl.allegro.tech.hermes.test.helper.avro;

import org.apache.avro.Schema;

import java.io.IOException;
import java.io.UncheckedIOException;

public class AvroUserSchemaLoader {

    public static Schema load() {
        return load("/schema/user.avsc");
    }

    public static Schema load(String schemaResourceName) {
        try {
            return new Schema.Parser().parse(AvroUserSchemaLoader.class.getResourceAsStream(schemaResourceName));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
