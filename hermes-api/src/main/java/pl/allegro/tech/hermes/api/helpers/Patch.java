package pl.allegro.tech.hermes.api.helpers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class Patch {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.WRITE_NULL_MAP_VALUES);

    @SuppressWarnings("unchecked")
    public static <T1, T2> T1 apply(T1 left, T2 right) {
        checkNotNull(left); checkNotNull(right);
        Map leftMap = MAPPER.convertValue(left, Map.class);
        Map rightMap = MAPPER.convertValue(right, Map.class);
        return (T1) MAPPER.convertValue(merge(leftMap, rightMap), left.getClass());
    }

    @SuppressWarnings("unchecked")
    private static Map merge(Map<String, Object> left, Map<String, Object> right) {
        Map<String, Object> merged = new HashMap<>(left);
        for (Map.Entry<String, Object> entry : right.entrySet()) {
            if (entry.getValue() instanceof Map && merged.containsKey(entry.getKey())) {
                Map<String, Object> nested = (Map) merged.get(entry.getKey());
                nested.putAll(merge(nested, (Map) entry.getValue()));
            } else {
                merged.put(entry.getKey(), entry.getValue());
            }
        }
        return merged;
    }

}
