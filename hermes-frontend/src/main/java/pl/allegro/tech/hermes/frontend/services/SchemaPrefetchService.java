package pl.allegro.tech.hermes.frontend.services;

import org.apache.commons.lang3.exception.ExceptionUtils;
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

    private static final Logger logger = LoggerFactory.getLogger(SchemaPrefetchService.class);

    @Inject
    public SchemaPrefetchService(List<SchemaRepository> schemaRepositories) {
        this.schemaRepositories = schemaRepositories;
    }

    public void prefetchFor(Topic topic) {
        schemaRepositories.stream().forEach(repo -> {
            try {
                repo.getSchema(topic);
                logger.info("Successful prefetch of schema for topic {} with content type {} via {} schema repo", topic.getQualifiedName(), topic.getContentType(), repo.supportedContentType());
            } catch (Exception exception) {
                logger.info("Unsuccessful prefetch of schema for topic {} with content type {} via {} schema repo. Root cause: {}",
                    topic.getQualifiedName(),
                    topic.getContentType(),
                    repo.supportedContentType(),
                    ExceptionUtils.getRootCauseMessage(exception));
            }
        });
    }
}
