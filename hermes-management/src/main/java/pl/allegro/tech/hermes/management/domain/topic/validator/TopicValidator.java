package pl.allegro.tech.hermes.management.domain.topic.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.PublishingChaosPolicy;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.management.api.validator.ApiPreconditions;
import pl.allegro.tech.hermes.management.domain.auth.RequestUser;
import pl.allegro.tech.hermes.management.domain.owner.validator.OwnerIdValidator;
import pl.allegro.tech.hermes.management.domain.topic.CreatorRights;
import pl.allegro.tech.hermes.schema.CouldNotLoadSchemaException;
import pl.allegro.tech.hermes.schema.SchemaNotFoundException;
import pl.allegro.tech.hermes.schema.SchemaRepository;

@Component
public class TopicValidator {

    private final OwnerIdValidator ownerIdValidator;
    private final ContentTypeValidator contentTypeValidator;
    private final TopicLabelsValidator topicLabelsValidator;
    private final SchemaRepository schemaRepository;
    private final ApiPreconditions apiPreconditions;

    @Autowired
    public TopicValidator(OwnerIdValidator ownerIdValidator,
                          ContentTypeValidator contentTypeValidator,
                          TopicLabelsValidator topicLabelsValidator,
                          SchemaRepository schemaRepository,
                          ApiPreconditions apiPreconditions) {
        this.ownerIdValidator = ownerIdValidator;
        this.contentTypeValidator = contentTypeValidator;
        this.topicLabelsValidator = topicLabelsValidator;
        this.schemaRepository = schemaRepository;
        this.apiPreconditions = apiPreconditions;
    }

    public void ensureCreatedTopicIsValid(Topic created, RequestUser createdBy, CreatorRights creatorRights) {
        apiPreconditions.checkConstraints(created, createdBy.isAdmin());
        checkOwner(created);
        checkContentType(created);
        checkTopicLabels(created);

        if (created.isFallbackToRemoteDatacenterEnabled() && !createdBy.isAdmin()) {
            throw new TopicValidationException("User is not allowed to enable fallback to remote datacenter");
        }

        if (created.getChaos().enabled() && !createdBy.isAdmin()) {
            throw new TopicValidationException("User is not allowed to set chaos policy for this topic");
        }
        validateChaosPolicy(created.getChaos());

        if (created.wasMigratedFromJsonType()) {
            throw new TopicValidationException("Newly created topic cannot have migratedFromJsonType flag set to true");
        }

        if (!creatorRights.allowedToManage(created)) {
            throw new TopicValidationException("Provide an owner that includes you, you would not be able to manage this topic later");
        }
    }

    public void ensureUpdatedTopicIsValid(Topic updated, Topic previous, RequestUser modifiedBy) {
        apiPreconditions.checkConstraints(updated, modifiedBy.isAdmin());
        checkOwner(updated);
        checkTopicLabels(updated);

        if (!previous.isFallbackToRemoteDatacenterEnabled() && updated.isFallbackToRemoteDatacenterEnabled() && !modifiedBy.isAdmin()) {
            throw new TopicValidationException("User is not allowed to enable fallback to remote datacenter");
        }

        if (!previous.getChaos().equals(updated.getChaos()) && !modifiedBy.isAdmin()) {
            throw new TopicValidationException("User is not allowed to update chaos policy for this topic");
        }
        validateChaosPolicy(updated.getChaos());

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
            throw new TopicValidationException(
                    "Cannot change content type, except for migration to Avro with setting migratedFromJsonType flag.");
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

    private void checkOwner(Topic checked) {
        ownerIdValidator.check(checked.getOwner());
    }

    private void checkContentType(Topic checked) {
        contentTypeValidator.check(checked.getContentType());
    }

    private void checkTopicLabels(Topic checked) {
        topicLabelsValidator.check(checked.getLabels());
    }

    private void validateChaosPolicy(PublishingChaosPolicy chaosPolicy) {
        boolean anyInvalid = chaosPolicy.datacenterChaosPolicies().values().stream()
                .anyMatch(datacenterChaosPolicy -> {
                    if (datacenterChaosPolicy.delayFrom() < 0 || datacenterChaosPolicy.delayTo() < 0) {
                        return true;
                    }
                    return datacenterChaosPolicy.delayFrom() > datacenterChaosPolicy.delayTo();
                });
        if (anyInvalid) {
            throw new TopicValidationException("Invalid chaos policy: 'delayFrom' and 'delayTo' must be >= 0, and 'delayFrom' <= 'delayTo'.");
        }
    }
}
