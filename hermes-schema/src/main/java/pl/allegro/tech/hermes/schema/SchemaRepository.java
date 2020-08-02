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
        return getCompiledSchemaAtVersion(topic, latestVersion);
    }

    public CompiledSchema<Schema> getAvroSchema(Topic topic, SchemaVersion version) {
        SchemaVersionsResponse response = schemaVersionsRepository.versions(topic);
        if (response.isFailure()) {
            throw new SchemaNotFoundException(topic, version);
        }
        if (!response.versionExists(version)) {
            throw new SchemaVersionDoesNotExistException(topic, version);
        }
        return getCompiledSchemaAtVersion(topic, version);
    }

    public CompiledSchema<Schema> getAvroSchema(Topic topic, SchemaId id) {
        CompiledSchema<Schema> schema = compiledAvroSchemaRepository.getSchema(topic, id);

        if (schema == null) {
            throw new SchemaNotFoundException(id);
        }

        return schema;
    }

    /**
     * This method should be used only for cache-backed repository implementations
     * where we have possibly stale versions-cache and are 100% sure the requested version exists.
     * If it does not exist, each method call will try to load the requested version
     * from underlying schema repository.
     */
    public CompiledSchema<Schema> getKnownAvroSchemaVersion(Topic topic, SchemaVersion version) {
        return getCompiledSchemaAtVersion(topic, version);
    }

    private CompiledSchema<Schema> getCompiledSchemaAtVersion(Topic topic, SchemaVersion latestVersion) {
        try {
            return compiledAvroSchemaRepository.getSchema(topic, latestVersion);
        } catch (CouldNotLoadSchemaException e) {
            throw e;
        } catch (Exception e) {
            throw new CouldNotLoadSchemaException(topic, latestVersion, e);
        }
    }

    public List<SchemaVersion> getVersions(Topic topic, boolean online) {
        return schemaVersionsRepository.versions(topic, online).get();
    }
}
