package pl.allegro.tech.hermes.consumers.consumer.converter.schema;

import com.google.common.collect.Maps;
import org.apache.avro.Schema;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.domain.topic.schema.CouldNotLoadSchemaException;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository;

import javax.inject.Inject;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static pl.allegro.tech.hermes.common.message.wrapper.AvroMetadataMarker.METADATA_MARKER;

public class AvroSchemaRepositoryMetadataAware {

    private final SchemaRepository<Schema> avroSchemaRepository;
    private final Map<Topic, Schema> schemasWithoutMetadata = Maps.newConcurrentMap();

    @Inject
    public AvroSchemaRepositoryMetadataAware(SchemaRepository<Schema> avroSchemaRepository) {
        this.avroSchemaRepository = avroSchemaRepository;
        avroSchemaRepository.onReload(topicWithSchema ->
                schemasWithoutMetadata.put(topicWithSchema.getTopic(), removeMetadataField(topicWithSchema.getSchema())));
        avroSchemaRepository.onRemove(topicWithSchema ->
                schemasWithoutMetadata.remove(topicWithSchema.getTopic()));
    }

    public Schema getSchemaWithoutMetadata(Topic topic) {
        try {
            return schemasWithoutMetadata.computeIfAbsent(topic, this::loadWithoutMetadata);
        } catch (Exception e) {
            throw new CouldNotLoadSchemaException("Could not load schema without metadata for topic " + topic.getQualifiedName(), e);
        }
    }

    private Schema loadWithoutMetadata(Topic topic) {
        return removeMetadataField(avroSchemaRepository.getSchema(topic));
    }

    private Schema removeMetadataField(Schema schema) {
        return Schema.createRecord(
            schema.getFields().stream()
                .filter(field -> !METADATA_MARKER.equals(field.name()))
                .map(field -> new Schema.Field(field.name(), field.schema(), field.doc(), field.defaultValue()))
                .collect(toList()));
    }
}
