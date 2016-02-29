package pl.allegro.tech.hermes.domain.topic.schema;

import com.github.fge.jsonschema.main.JsonSchema;
import org.apache.avro.Schema;
import pl.allegro.tech.hermes.api.Topic;

import javax.inject.Inject;

public class AggregateSchemaRepository {

    private final SchemaVersionsRepository schemaVersionsRepository;
    private final ConcreteSchemaRepository<Schema> avroSchemaRepository;
    private final ConcreteSchemaRepository<JsonSchema> jsonSchemaRepository;

    @Inject
    public AggregateSchemaRepository(SchemaVersionsRepository schemaVersionsRepository,
                                     ConcreteSchemaRepository<Schema> avroSchemaRepository,
                                     ConcreteSchemaRepository<JsonSchema> jsonSchemaRepository) {
        this.schemaVersionsRepository = schemaVersionsRepository;
        this.avroSchemaRepository = avroSchemaRepository;
        this.jsonSchemaRepository = jsonSchemaRepository;
    }

    public VersionedSchema<Schema> getAvroSchema(Topic topic) {
        return getSchema(topic, avroSchemaRepository);
    }

    public VersionedSchema<Schema> getAvroSchema(Topic topic, int version) {
        return getSchema(topic, version, avroSchemaRepository);
    }

    public VersionedSchema<JsonSchema> getJsonSchema(Topic topic) {
        return getSchema(topic, jsonSchemaRepository);
    }

    public VersionedSchema<JsonSchema> getJsonSchema(Topic topic, int version) {
        return getSchema(topic, version, jsonSchemaRepository);
    }

    private <T> VersionedSchema<T> getSchema(Topic topic, ConcreteSchemaRepository<T> concreteSchemaRepository) {
        int latestVersion = schemaVersionsRepository.latestSchemaVersion(topic).orElseThrow(() -> new SchemaMissingException(topic));
        return getSchema(topic, latestVersion, concreteSchemaRepository);
    }

    private <T> VersionedSchema<T> getSchema(Topic topic, int version, ConcreteSchemaRepository<T> concreteSchemaRepository) {
        if (!schemaVersionsRepository.schemaVersionExists(topic, version)) {
            throw new UnknownSchemaVersionException(topic, version);
        }

        return concreteSchemaRepository.getSchema(topic, version);

    }

}
