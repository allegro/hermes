package pl.allegro.tech.hermes.schema;

import org.apache.avro.Schema;
import pl.allegro.tech.hermes.api.Topic;

import java.util.List;

public class SchemaRepository {

    private final SchemaVersionsRepository schemaVersionsRepository;
    private final CompiledSchemaRepository<Schema> compiledAvroSchemaRepository;

    public SchemaRepository(SchemaVersionsRepository schemaVersionsRepository,
                            CompiledSchemaRepository<Schema> compiledAvroSchemaRepository) {
        this.schemaVersionsRepository = schemaVersionsRepository;
        this.compiledAvroSchemaRepository = compiledAvroSchemaRepository;
    }

    public CompiledSchema<Schema> getLatestAvroSchema(Topic topic) {
        SchemaVersion latestVersion = schemaVersionsRepository.latestSchemaVersion(topic)
                .orElseThrow(() -> new SchemaNotFoundException(topic));
        if (!schemaVersionsRepository.schemaVersionExists(topic, latestVersion)) {
            throw new SchemaNotFoundException(topic, latestVersion);
        }
        return compiledAvroSchemaRepository.getSchema(topic, latestVersion);
    }

    public CompiledSchema<Schema> getAvroSchema(Topic topic, SchemaVersion version) {
        if (!schemaVersionsRepository.schemaVersionExists(topic, version)) {
            throw new SchemaNotFoundException(topic, version);
        }
        return compiledAvroSchemaRepository.getSchema(topic, version);
    }

    /**
     * This method should be used only for cache-backed repository implementations
     * where we have possibly stale versions-cache and are 100% sure the requested version exists.
     * If it does not exist, each method call will try to load the requested version
     * from underlying schema repository.
     */
    public CompiledSchema<Schema> getKnownAvroSchemaVersion(Topic topic, SchemaVersion version) {
        return compiledAvroSchemaRepository.getSchema(topic, version);
    }

    public List<SchemaVersion> getVersions(Topic topic, boolean online) {
        return schemaVersionsRepository.versions(topic, online);
    }
}
