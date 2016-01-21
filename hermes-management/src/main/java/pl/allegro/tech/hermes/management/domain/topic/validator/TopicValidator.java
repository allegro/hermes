package pl.allegro.tech.hermes.management.domain.topic.validator;

import org.apache.avro.Schema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.domain.topic.schema.CouldNotLoadSchemaException;
import pl.allegro.tech.hermes.domain.topic.schema.SchemaRepository;

@Component
public class TopicValidator {

    private final SchemaRepository<Schema> avroSchemaRepository;

    @Autowired
    public TopicValidator(SchemaRepository<Schema> avroSchemaRepository) {
        this.avroSchemaRepository = avroSchemaRepository;
    }

    public void ensureCreatedTopicIsValid(Topic created) {
        if (created.wasMigratedFromJsonType()) {
            throw new TopicValidationException("Newly created topic cannot have migratedFromJsonType flag set to true");
        }
    }

    public void ensureUpdatedTopicIsValid(Topic updated, Topic previous) {
        if (migrationFromJsonTypeFlagChangedToTrue(updated, previous)) {
            if (updated.getContentType() != ContentType.AVRO) {
                throw new TopicValidationException("Change content type to AVRO together with setting migratedFromJsonType flag");
            }

            try {
                avroSchemaRepository.getSchema(updated);
            } catch (CouldNotLoadSchemaException e) {
                throw new TopicValidationException("Avro schema not available, migration not permitted", e);
            }
        } else if (contentTypeChanged(updated, previous)) {
            throw new TopicValidationException("Cannot change content type, except for migration to Avro with setting migratedFromJsonType flag.");
        } else if (migrationFromJsonTypeFlagChangedToFalse(updated, previous)) {
            throw new TopicValidationException("Cannot migrate back to JSON!");
        }
    }

    private boolean contentTypeChanged(Topic updated, Topic previous) {
        return previous.getContentType() != updated.getContentType();
    }

    private boolean migrationFromJsonTypeFlagChangedToTrue(Topic updated, Topic previous) {
        return !previous.wasMigratedFromJsonType() && updated.wasMigratedFromJsonType();
    }

    private boolean migrationFromJsonTypeFlagChangedToFalse(Topic updated, Topic previous) {
        return previous.wasMigratedFromJsonType() && !updated.wasMigratedFromJsonType();
    }

}
