package pl.allegro.tech.hermes.common.json;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

public class JsonReader {

    static ObjectMapper mapper = new ObjectMapper();

    @SuppressWarnings("unchecked")
    public static Map<String, Object> readMap(byte[] result) {
        try {
            return mapper.readValue(new String(result), Map.class);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
