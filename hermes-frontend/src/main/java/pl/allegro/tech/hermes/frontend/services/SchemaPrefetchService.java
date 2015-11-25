package pl.allegro.tech.hermes.frontend.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.domain.topic.schema.CachedSchemaSourceProvider;
import pl.allegro.tech.hermes.domain.topic.schema.CouldNotLoadSchemaException;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository;

import javax.inject.Inject;
import java.util.List;

public class SchemaPrefetchService {
    private static final Logger logger = LoggerFactory.getLogger(SchemaPrefetchService.class);

    private final CachedSchemaSourceProvider cachedSchemaSourceProvider;
    private final List<SchemaRepository> schemaRepositories;

    @Inject
    public SchemaPrefetchService(List<SchemaRepository> schemaRepositories, CachedSchemaSourceProvider cachedSchemaSourceProvider) {
        this.schemaRepositories = schemaRepositories;
        this.cachedSchemaSourceProvider = cachedSchemaSourceProvider;
    }

    public void prefetchFor(Topic topic) {
        cachedSchemaSourceProvider.get(topic).ifPresent(schema -> {
            logger.info("Successful schema source prefetch for topic {}", topic.getQualifiedName());
            compileSchema(topic);
        });
    }

    private void compileSchema(Topic topic) {
        schemaRepositories.forEach(schemaRepository -> {
            try {
                schemaRepository.getSchema(topic);
                logger.info("Successful schema compilation type of {} for topic {}",
                        schemaRepository.supportedContentType(), topic.getQualifiedName());
            } catch (CouldNotLoadSchemaException exception) {
                logger.debug("Unsuccessful schema compilation type of {} for topic {}",
                        schemaRepository.supportedContentType(), topic.getQualifiedName());
            }
        });
    }
}
