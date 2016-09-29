package pl.allegro.tech.hermes.schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;

import java.util.List;

import static java.util.Collections.emptyList;

public class DirectSchemaVersionsRepository implements SchemaVersionsRepository {

    private static final Logger logger = LoggerFactory.getLogger(DirectSchemaVersionsRepository.class);

    private final SchemaSourceClient schemaSourceClient;

    public DirectSchemaVersionsRepository(SchemaSourceClient schemaSourceClient) {
        this.schemaSourceClient = schemaSourceClient;
    }

    @Override
    public List<SchemaVersion> versions(Topic topic, boolean online) {
        try {
            return schemaSourceClient.getVersions(topic.getName());
        } catch (Exception e) {
            logger.error("Error while loading schema versions for topic {}", topic.getQualifiedName(), e);
            return emptyList();
        }
    }
}
