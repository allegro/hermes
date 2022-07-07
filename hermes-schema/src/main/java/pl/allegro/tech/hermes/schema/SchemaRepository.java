package pl.allegro.tech.hermes.schema;

import org.apache.avro.Schema;
import pl.allegro.tech.hermes.api.Topic;

import java.util.List;

public class SchemaRepository {

    private static final boolean OFFLINE = false;

    private final SchemaVersionsRepository schemaVersionsRepository;
    private final CompiledSchemaRepository<Schema> compiledAvroSchemaRepository;
    private final SchemaCacheRefresher<Schema> schemaCacheRefresher;

    public SchemaRepository(SchemaVersionsRepository schemaVersionsRepository,
                            CompiledSchemaRepository<Schema> compiledAvroSchemaRepository) {
        this.schemaVersionsRepository = schemaVersionsRepository;
        this.compiledAvroSchemaRepository = compiledAvroSchemaRepository;
        this.schemaCacheRefresher = new SchemaCacheRefresher<>(schemaVersionsRepository, compiledAvroSchemaRepository);
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
        SchemaVersionsResult result = schemaVersionsRepository.versions(topic);
        if (result.isFailure()) {
            throw new SchemaNotFoundException(topic, version);
        }
        if (!result.versionExists(version)) {
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
        if (online) {
            schemaCacheRefresher.refreshSchemas(topic);
        }
        SchemaVersionsResult versionsResult = schemaVersionsRepository.versions(topic, OFFLINE);

        return versionsResult.get();
    }
}
