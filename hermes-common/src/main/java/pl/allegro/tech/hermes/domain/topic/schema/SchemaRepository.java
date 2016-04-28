package pl.allegro.tech.hermes.domain.topic.schema;

import com.github.fge.jsonschema.main.JsonSchema;
import org.apache.avro.Schema;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.common.message.AvroSchemaSource;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

public class SchemaRepository implements AvroSchemaSource {

    private final SchemaVersionsRepository schemaVersionsRepository;
    private final CompiledSchemaRepository<Schema> avroSchemaRepository;
    private final CompiledSchemaRepository<JsonSchema> jsonSchemaRepository;

    @Inject
    public SchemaRepository(SchemaVersionsRepository schemaVersionsRepository,
                            CompiledSchemaRepository<Schema> avroSchemaRepository,
                            CompiledSchemaRepository<JsonSchema> jsonSchemaRepository) {
        this.schemaVersionsRepository = schemaVersionsRepository;
        this.avroSchemaRepository = avroSchemaRepository;
        this.jsonSchemaRepository = jsonSchemaRepository;
    }

    public CompiledSchema<Schema> getAvroSchema(Topic topic) {
        return getSchema(topic, avroSchemaRepository);
    }

    public CompiledSchema<Schema> getAvroSchema(Topic topic, SchemaVersion version) {
        return getSchema(topic, version, avroSchemaRepository);
    }

    public CompiledSchema<JsonSchema> getJsonSchema(Topic topic) {
        return getSchema(topic, jsonSchemaRepository);
    }

    public CompiledSchema<JsonSchema> getJsonSchema(Topic topic, SchemaVersion version) {
        return getSchema(topic, version, jsonSchemaRepository);
    }

    private <T> CompiledSchema<T> getSchema(Topic topic, CompiledSchemaRepository<T> compiledSchemaRepository) {
        SchemaVersion latestVersion = schemaVersionsRepository.latestSchemaVersion(topic).orElseThrow(() -> new SchemaMissingException(topic));
        return getSchema(topic, latestVersion, compiledSchemaRepository);
    }

    private <T> CompiledSchema<T> getSchema(Topic topic, SchemaVersion version, CompiledSchemaRepository<T> compiledSchemaRepository) {
        if (!schemaVersionsRepository.schemaVersionExists(topic, version)) {
            throw new UnknownSchemaVersionException(topic, version);
        }
        return compiledSchemaRepository.getSchema(topic, version);
    }

    @Override
    public List<SchemaVersion> versions(Topic topic, boolean online) {
        return schemaVersionsRepository.versions(topic, online);
    }
}
