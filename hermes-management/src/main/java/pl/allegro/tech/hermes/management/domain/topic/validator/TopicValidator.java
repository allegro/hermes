package pl.allegro.tech.hermes.management.domain.topic.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.management.api.validator.ApiPreconditions;
import pl.allegro.tech.hermes.management.domain.maintainer.validator.MaintainerDescriptorValidator;
import pl.allegro.tech.hermes.schema.CouldNotLoadSchemaException;
import pl.allegro.tech.hermes.schema.SchemaNotFoundException;
import pl.allegro.tech.hermes.schema.SchemaRepository;

@Component
public class TopicValidator {

    private final MaintainerDescriptorValidator maintainerDescriptorValidator;
    private final SchemaRepository schemaRepository;
    private final ApiPreconditions apiPreconditions;

    @Autowired
    public TopicValidator(MaintainerDescriptorValidator maintainerDescriptorValidator,
                          SchemaRepository schemaRepository,
                          ApiPreconditions apiPreconditions) {
        this.maintainerDescriptorValidator = maintainerDescriptorValidator;
        this.schemaRepository = schemaRepository;
        this.apiPreconditions = apiPreconditions;
    }

    public void ensureCreatedTopicIsValid(Topic created) {
        apiPreconditions.checkConstraints(created);
        checkMaintainer(created);

        if (created.wasMigratedFromJsonType()) {
            throw new TopicValidationException("Newly created topic cannot have migratedFromJsonType flag set to true");
        }
    }

    public void ensureUpdatedTopicIsValid(Topic updated, Topic previous) {
        apiPreconditions.checkConstraints(updated);
        checkMaintainer(updated);

        if (migrationFromJsonTypeFlagChangedToTrue(updated, previous)) {
            if (updated.getContentType() != ContentType.AVRO) {
                throw new TopicValidationException("Change content type to AVRO together with setting migratedFromJsonType flag");
            }

            try {
                schemaRepository.getLatestAvroSchema(updated);
            } catch (CouldNotLoadSchemaException | SchemaNotFoundException e) {
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

    private void checkMaintainer(Topic checked) {
        maintainerDescriptorValidator.check(checked.getMaintainer());
    }

}
