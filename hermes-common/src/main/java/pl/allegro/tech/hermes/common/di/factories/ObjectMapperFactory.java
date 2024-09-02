package pl.allegro.tech.hermes.common.di.factories;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import pl.allegro.tech.hermes.api.Topic;

public class ObjectMapperFactory {

  private final boolean schemaIdSerializationEnabled;
  private final boolean fallbackToRemoteDatacenterEnabled;

  public ObjectMapperFactory(
      boolean schemaIdSerializationEnabled, boolean fallbackToRemoteDatacenterEnabled) {
    this.schemaIdSerializationEnabled = schemaIdSerializationEnabled;
    this.fallbackToRemoteDatacenterEnabled = fallbackToRemoteDatacenterEnabled;
  }

  public ObjectMapper provide() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    objectMapper.disable(SerializationFeature.WRITE_NULL_MAP_VALUES);
    objectMapper.registerModule(new JavaTimeModule());

    final InjectableValues defaultSchemaIdAwareSerializationEnabled =
        new InjectableValues.Std()
            .addValue(
                Topic.DEFAULT_SCHEMA_ID_SERIALIZATION_ENABLED_KEY, schemaIdSerializationEnabled)
            .addValue(
                Topic.DEFAULT_FALLBACK_TO_REMOTE_DATACENTER_KEY, fallbackToRemoteDatacenterEnabled);
    objectMapper.setInjectableValues(defaultSchemaIdAwareSerializationEnabled);

    return objectMapper;
  }
}
