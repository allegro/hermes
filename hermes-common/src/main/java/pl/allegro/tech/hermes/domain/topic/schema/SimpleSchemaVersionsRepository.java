package pl.allegro.tech.hermes.domain.topic.schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

public class SimpleSchemaVersionsRepository extends AbstractSchemaVersionsRepository {

    private static final Logger logger = LoggerFactory.getLogger(SimpleSchemaVersionsRepository.class);

    private final SchemaSourceProvider schemaSourceProvider;

    @Inject
    public SimpleSchemaVersionsRepository(SchemaSourceProvider schemaSourceProvider) {
        this.schemaSourceProvider = schemaSourceProvider;
    }

    @Override
    protected Optional<List<Integer>> versions(Topic topic) {
        try {
            return Optional.of(schemaSourceProvider.versions(topic));
        } catch (Exception e) {
            logger.error("Error while loading schema versions for topic {}", topic.getQualifiedName(), e);
            return Optional.empty();
        }
    }
}
