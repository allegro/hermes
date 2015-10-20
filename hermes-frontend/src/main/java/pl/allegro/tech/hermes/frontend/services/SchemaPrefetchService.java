package pl.allegro.tech.hermes.frontend.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

@Singleton
public class SchemaPrefetchService {
    private List<SchemaRepository> schemaRepositories;

    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaPrefetchService.class);

    @Inject
    public SchemaPrefetchService(List<SchemaRepository> schemaRepositories) {
        this.schemaRepositories = schemaRepositories;
    }

    public void prefetchFor(Topic topic) {
        if (topic.isValidationEnabled()) {
            LOGGER.debug("Prefetching schema for topic {}", topic.getQualifiedName());
            prefetch(topic);
        }
    }

    private void prefetch(Topic topic) {
        schemaRepositories.stream()
                .filter(repo -> repo.canService(topic.getContentType()))
                .forEach(repo -> repo.getSchema(topic));
    }
}
