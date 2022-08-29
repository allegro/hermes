package pl.allegro.tech.hermes.test.helper.avro;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class AvroUserIdlLoader {

    public static String load() {
        return load("/schema/user.avdl");
    }

    public static String load(String schemaResourceName) {
        try {
            InputStream inputStream = AvroUserIdlLoader.class.getResourceAsStream(schemaResourceName);
            return new String(Objects.requireNonNull(inputStream).readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
