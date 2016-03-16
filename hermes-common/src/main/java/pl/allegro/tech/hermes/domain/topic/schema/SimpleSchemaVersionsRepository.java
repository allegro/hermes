package pl.allegro.tech.hermes.domain.topic.schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;

import javax.inject.Inject;
import java.util.List;

import static java.util.Collections.emptyList;

public class SimpleSchemaVersionsRepository implements SchemaVersionsRepository {

    private static final Logger logger = LoggerFactory.getLogger(SimpleSchemaVersionsRepository.class);

    private final SchemaSourceProvider schemaSourceProvider;

    @Inject
    public SimpleSchemaVersionsRepository(SchemaSourceProvider schemaSourceProvider) {
        this.schemaSourceProvider = schemaSourceProvider;
    }

    @Override
    public List<SchemaVersion> versions(Topic topic) {
        try {
            return schemaSourceProvider.versions(topic);
        } catch (Exception e) {
            logger.error("Error while loading schema versions for topic {}", topic.getQualifiedName(), e);
            return emptyList();
        }
    }
}
