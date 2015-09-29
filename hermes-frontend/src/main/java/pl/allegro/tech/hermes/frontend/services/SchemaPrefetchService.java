package pl.allegro.tech.hermes.frontend.services;

import com.github.fge.jsonschema.main.JsonSchema;
import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SchemaPrefetchService {
    private SchemaRepository<Schema> avroSchemaRepo;
    private SchemaRepository<JsonSchema> jsonSchemaRepo;

    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaPrefetchService.class);

    @Inject
    public SchemaPrefetchService(SchemaRepository<Schema> avroSchemaRepo, SchemaRepository<JsonSchema> jsonSchemaRepo) {
        this.avroSchemaRepo = avroSchemaRepo;
        this.jsonSchemaRepo = jsonSchemaRepo;
    }

    public void prefetchFor(Topic topic) {
        if (topic.isValidationEnabled()) {
            LOGGER.debug("Prefetching schema for topic {}", topic.getQualifiedName());
            prefetch(topic);
        }
    }

    private void prefetch(Topic topic) {
        switch (topic.getContentType()) {
            case AVRO:
                avroSchemaRepo.getSchema(topic);
                break;
            case JSON:
                jsonSchemaRepo.getSchema(topic);
                break;
            default:
                LOGGER.warn("Schema prefetch failed for topic {}: unknown content type {}", topic.getQualifiedName(),
                        topic.getContentType());
                break;
        }
    }
}
