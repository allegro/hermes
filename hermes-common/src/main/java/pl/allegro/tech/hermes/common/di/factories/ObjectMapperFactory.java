package pl.allegro.tech.hermes.common.di.factories;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.glassfish.hk2.api.Factory;

public class ObjectMapperFactory implements Factory<ObjectMapper> {

    @Override
    public ObjectMapper provide() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.disable(SerializationFeature.WRITE_NULL_MAP_VALUES);
        return objectMapper;
    }

    @Override
    public void dispose(ObjectMapper instance) {

    }
}
