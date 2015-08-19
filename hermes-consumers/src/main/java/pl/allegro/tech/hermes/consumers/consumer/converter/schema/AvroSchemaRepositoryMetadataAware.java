package pl.allegro.tech.hermes.consumers.consumer.converter.schema;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.avro.Schema;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.domain.topic.schema.CouldNotLoadSchemaException;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository;

import static com.google.common.base.Ticker.systemTicker;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.Collectors.toList;
import static pl.allegro.tech.hermes.common.message.wrapper.AvroMetadataMarker.METADATA_MARKER;

public class AvroSchemaRepositoryMetadataAware {

    private final SchemaRepository<Schema> avroSchemaRepository;
    private final LoadingCache<Topic, Schema> schemaWithoutMetadataCache;

    public AvroSchemaRepositoryMetadataAware(SchemaRepository<Schema> avroSchemaRepository, int schemaCacheRefreshAfterWriteMinutes,
                                             int schemaCacheExpireAfterWriteMinutes) {
        this.avroSchemaRepository = avroSchemaRepository;

        schemaWithoutMetadataCache = CacheBuilder
                .newBuilder()
                .ticker(systemTicker())
                .refreshAfterWrite(schemaCacheRefreshAfterWriteMinutes, MINUTES)
                .expireAfterWrite(schemaCacheExpireAfterWriteMinutes, MINUTES)
                .build(new SchemaWithoutMetadataCacheLoader());
    }

    public Schema getSchemaWithoutMetadata(Topic topic) {
        try {
            return schemaWithoutMetadataCache.get(topic);
        } catch (Exception e) {
            throw new CouldNotLoadSchemaException("Could not load schema for topic " + topic.getQualifiedName(), e);
        }
    }

    private class SchemaWithoutMetadataCacheLoader extends CacheLoader<Topic, Schema> {
        @Override
        public Schema load(Topic topic) throws Exception {
            return
                Schema.createRecord(
                    avroSchemaRepository.getSchema(topic).getFields().stream()
                        .filter(field -> !METADATA_MARKER.equals(field.name()))
                        .map(field -> new Schema.Field(field.name(), field.schema(), field.doc(), field.defaultValue()))
                        .collect(toList()));
        }
    }
}
