package pl.allegro.tech.hermes.consumers.consumer.converter.schema;

import com.google.common.collect.Maps;
import org.apache.avro.Schema;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.domain.topic.schema.CouldNotLoadSchemaException;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository;

import javax.inject.Inject;
import java.util.Map;
import java.util.Objects;

import static java.util.stream.Collectors.toList;
import static pl.allegro.tech.hermes.common.message.wrapper.AvroMetadataMarker.METADATA_MARKER;

public class AvroSchemaRepositoryMetadataAware {

    private final SchemaRepository<Schema> avroSchemaRepository;
    private final Map<Topic, Schemas> schemas = Maps.newConcurrentMap();

    @Inject
    public AvroSchemaRepositoryMetadataAware(SchemaRepository<Schema> avroSchemaRepository) {
        this.avroSchemaRepository = avroSchemaRepository;
        avroSchemaRepository.onReload(topicWithSchema -> schemas.put(topicWithSchema.getTopic(), new Schemas(topicWithSchema.getSchema())));
        avroSchemaRepository.onRemove(topicWithSchema -> schemas.remove(topicWithSchema.getTopic()));
    }

    public Schema getSchema(Topic topic) {
        try {
            return schemas.computeIfAbsent(topic, this::loadSchemas).getSchema();
        } catch (Exception e) {
            throw new CouldNotLoadSchemaException("Could not load schema for topic " + topic.getQualifiedName(), e);
        }
    }

    public Schema getSchemaWithoutMetadata(Topic topic) {
        try {
            return schemas.computeIfAbsent(topic, this::loadSchemas).getSchemaWithoutMetadata();
        } catch (Exception e) {
            throw new CouldNotLoadSchemaException("Could not load schema without metadata for topic " + topic.getQualifiedName(), e);
        }
    }

    private Schemas loadSchemas(Topic topic) {
        return new Schemas(avroSchemaRepository.getSchema(topic));
    }

    private static final class Schemas {
        private final Schema schema;
        private final Schema schemaWithoutMetadata;

        private Schemas(Schema schema) {
            this.schema = schema;
            this.schemaWithoutMetadata = removeMetadataField(schema);
        }

        private Schema removeMetadataField(Schema schema) {
            return Schema.createRecord(
                    schema.getFields().stream()
                            .filter(field -> !METADATA_MARKER.equals(field.name()))
                            .map(field -> new Schema.Field(field.name(), field.schema(), field.doc(), field.defaultValue()))
                            .collect(toList()));
        }

        public Schema getSchemaWithoutMetadata() {
            return schemaWithoutMetadata;
        }

        public Schema getSchema() {
            return schema;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Schemas)) return false;
            Schemas schemas = (Schemas) o;
            return Objects.equals(schema, schemas.schema) && Objects.equals(schemaWithoutMetadata, schemas.schemaWithoutMetadata);
        }

        @Override
        public int hashCode() {
            return Objects.hash(schema, schemaWithoutMetadata);
        }
    }
}
