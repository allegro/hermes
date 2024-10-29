package pl.allegro.tech.hermes.management.domain.topic.validator;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.allegro.tech.hermes.api.ContentType;
import pl.allegro.tech.hermes.api.PublishingChaosPolicy;
import pl.allegro.tech.hermes.api.PublishingChaosPolicy.ChaosMode;
import pl.allegro.tech.hermes.api.PublishingChaosPolicy.ChaosPolicy;
import pl.allegro.tech.hermes.api.RetentionTime;
import pl.allegro.tech.hermes.api.Topic;
import pl.allegro.tech.hermes.management.api.validator.ApiPreconditions;
import pl.allegro.tech.hermes.management.config.TopicProperties;
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
  private final TopicProperties topicProperties;

  @Autowired
  public TopicValidator(
      OwnerIdValidator ownerIdValidator,
      ContentTypeValidator contentTypeValidator,
      TopicLabelsValidator topicLabelsValidator,
      SchemaRepository schemaRepository,
      ApiPreconditions apiPreconditions,
      TopicProperties topicProperties) {
    this.ownerIdValidator = ownerIdValidator;
    this.contentTypeValidator = contentTypeValidator;
    this.topicLabelsValidator = topicLabelsValidator;
    this.schemaRepository = schemaRepository;
    this.apiPreconditions = apiPreconditions;
    this.topicProperties = topicProperties;
  }

  public void ensureCreatedTopicIsValid(
      Topic created, RequestUser createdBy, CreatorRights creatorRights) {
    apiPreconditions.checkConstraints(created, createdBy.isAdmin());
    checkOwner(created);
    checkContentType(created);
    checkTopicLabels(created);

    if ((created.isFallbackToRemoteDatacenterEnabled()
            != topicProperties.isDefaultFallbackToRemoteDatacenterEnabled())
        && !createdBy.isAdmin()) {
      throw new TopicValidationException(
          "User is not allowed to set non-default fallback to remote datacenter for this topic");
    }

    if (created.getChaos().mode() != ChaosMode.DISABLED && !createdBy.isAdmin()) {
      throw new TopicValidationException("User is not allowed to set chaos policy for this topic");
    }
    validateChaosPolicy(created.getChaos());

    if (created.wasMigratedFromJsonType()) {
      throw new TopicValidationException(
          "Newly created topic cannot have migratedFromJsonType flag set to true");
    }

    if (!creatorRights.allowedToManage(created)) {
      throw new TopicValidationException(
          "Provide an owner that includes you, you would not be able to manage this topic later");
    }

    ensureCreatedTopicRetentionTimeValid(created, createdBy);
  }

  public void ensureUpdatedTopicIsValid(Topic updated, Topic previous, RequestUser modifiedBy) {
    apiPreconditions.checkConstraints(updated, modifiedBy.isAdmin());
    checkOwner(updated);
    checkTopicLabels(updated);

    if ((previous.isFallbackToRemoteDatacenterEnabled()
            != updated.isFallbackToRemoteDatacenterEnabled())
        && !modifiedBy.isAdmin()) {
      throw new TopicValidationException(
          "User is not allowed to update fallback to remote datacenter for this topic");
    }

    if (!previous.getChaos().equals(updated.getChaos()) && !modifiedBy.isAdmin()) {
      throw new TopicValidationException(
          "User is not allowed to update chaos policy for this topic");
    }
    validateChaosPolicy(updated.getChaos());

    if (migrationFromJsonTypeFlagChangedToTrue(updated, previous)) {
      if (updated.getContentType() != ContentType.AVRO) {
        throw new TopicValidationException(
            "Change content type to AVRO together with setting migratedFromJsonType flag");
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

    ensureUpdatedTopicRetentionTimeValid(updated, previous, modifiedBy);
  }

  private void ensureCreatedTopicRetentionTimeValid(Topic created, RequestUser modifiedBy) {
    if (modifiedBy.isAdmin()) {
      return;
    }

    checkTopicRetentionTimeUnit(created.getRetentionTime().getRetentionUnit());

    long seconds =
        created
            .getRetentionTime()
            .getRetentionUnit()
            .toSeconds(created.getRetentionTime().getDuration());

    checkTopicRetentionLimit(seconds);
  }

  private void ensureUpdatedTopicRetentionTimeValid(
      Topic updated, Topic previous, RequestUser modifiedBy) {
    if (modifiedBy.isAdmin()) {
      return;
    }

    checkTopicRetentionTimeUnit(updated.getRetentionTime().getRetentionUnit());

    long updatedSeconds =
        updated
            .getRetentionTime()
            .getRetentionUnit()
            .toSeconds(updated.getRetentionTime().getDuration());
    long previousSeconds =
        previous
            .getRetentionTime()
            .getRetentionUnit()
            .toSeconds(previous.getRetentionTime().getDuration());

    if (updatedSeconds == previousSeconds) {
      return;
    }

    checkTopicRetentionLimit(updatedSeconds);
  }

  private void checkTopicRetentionTimeUnit(TimeUnit toCheck) {
    if (!RetentionTime.allowedUnits.contains(toCheck)) {
      throw new TopicValidationException(
          "Retention time unit must be one of: "
              + Arrays.toString(RetentionTime.allowedUnits.toArray()));
    }
  }

  private void checkTopicRetentionLimit(long retentionSeconds) {
    if (retentionSeconds
        > RetentionTime.MAX.getRetentionUnit().toSeconds(RetentionTime.MAX.getDuration())) {
      throw new TopicValidationException(
          "Retention time larger than 7 days can't be configured by non admin users");
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
    for (ChaosPolicy policy : chaosPolicy.datacenterPolicies().values()) {
      validate(policy);
    }
    validate(chaosPolicy.globalPolicy());
  }

  private void validate(ChaosPolicy chaosPolicy) {
    if (chaosPolicy == null) {
      return;
    }
    if (chaosPolicy.delayFrom() < 0
        || chaosPolicy.delayTo() < 0
        || chaosPolicy.delayFrom() > chaosPolicy.delayTo()) {
      throw new TopicValidationException(
          "Invalid chaos policy: 'delayFrom' and 'delayTo' must be >= 0, and 'delayFrom' <= 'delayTo'.");
    }
    if (chaosPolicy.probability() < 0 || chaosPolicy.probability() > 100) {
      throw new TopicValidationException(
          "Invalid chaos policy: 'probability' must be within the range 0 to 100");
    }
  }
}
