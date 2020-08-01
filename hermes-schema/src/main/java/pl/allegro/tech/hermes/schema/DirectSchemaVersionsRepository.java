package pl.allegro.tech.hermes.schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;

import java.util.List;

public class DirectSchemaVersionsRepository implements SchemaVersionsRepository {

    private static final Logger logger = LoggerFactory.getLogger(DirectSchemaVersionsRepository.class);

    private final RawSchemaClient rawSchemaClient;

    public DirectSchemaVersionsRepository(RawSchemaClient rawSchemaClient) {
        this.rawSchemaClient = rawSchemaClient;
    }

    @Override
    public SchemaVersionsResponse versions(Topic topic, boolean online) {
        try {
            List<SchemaVersion> versions = rawSchemaClient.getVersions(topic.getName());
            return SchemaVersionsResponse.succeeded(versions);
        } catch (Exception e) {
            logger.error("Error while loading schema versions for topic {}", topic.getQualifiedName(), e);
            return SchemaVersionsResponse.failed();
        }
    }

    @Override
    public void close() {
        // nothing to close
    }
}
