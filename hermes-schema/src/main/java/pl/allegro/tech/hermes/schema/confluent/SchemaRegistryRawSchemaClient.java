package pl.allegro.tech.hermes.schema.confluent;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.Family.SUCCESSFUL;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.RawSchema;
import pl.allegro.tech.hermes.api.RawSchemaWithMetadata;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.schema.BadSchemaRequestException;
import pl.allegro.tech.hermes.schema.InternalSchemaRepositoryException;
import pl.allegro.tech.hermes.schema.RawSchemaClient;
import pl.allegro.tech.hermes.schema.SchemaId;
import pl.allegro.tech.hermes.schema.SchemaVersion;
import pl.allegro.tech.hermes.schema.SubjectNamingStrategy;
import pl.allegro.tech.hermes.schema.resolver.SchemaRepositoryInstanceResolver;

/**
 * This implementation of RawSchemaClient is compatible with Confluent Schema Registry API except
 * for the deleteAllSchemaVersions and validation endpoint which are not fully supported by the
 * Confluent project.
 */
public class SchemaRegistryRawSchemaClient implements RawSchemaClient {

  private static final Logger logger = LoggerFactory.getLogger(SchemaRegistryRawSchemaClient.class);

  private static final String SCHEMA_REPO_CONTENT_TYPE = "application/vnd.schemaregistry.v1+json";

  private final SchemaRepositoryInstanceResolver schemaRepositoryInstanceResolver;

  private final ObjectMapper objectMapper;
  private final boolean validationEndpointEnabled;
  private final String deleteSchemaPathSuffix;
  private final SubjectNamingStrategy subjectNamingStrategy;

  public SchemaRegistryRawSchemaClient(
      SchemaRepositoryInstanceResolver schemaRepositoryInstanceResolver,
      ObjectMapper objectMapper,
      SubjectNamingStrategy subjectNamingStrategy) {
    this(schemaRepositoryInstanceResolver, objectMapper, false, "versions", subjectNamingStrategy);
  }

  public SchemaRegistryRawSchemaClient(
      SchemaRepositoryInstanceResolver schemaRepositoryInstanceResolver,
      ObjectMapper objectMapper,
      boolean validationEndpointEnabled,
      String deleteSchemaPathSuffix,
      SubjectNamingStrategy subjectNamingStrategy) {
    this.schemaRepositoryInstanceResolver = schemaRepositoryInstanceResolver;
    this.validationEndpointEnabled = validationEndpointEnabled;
    this.deleteSchemaPathSuffix = deleteSchemaPathSuffix;
    this.objectMapper = objectMapper;
    this.subjectNamingStrategy = subjectNamingStrategy;
  }

  @Override
  public Optional<RawSchemaWithMetadata> getRawSchemaWithMetadata(
      TopicName topic, SchemaVersion schemaVersion) {
    String version = Integer.toString(schemaVersion.value());
    String subject = subjectNamingStrategy.apply(topic);
    Response response = getRawSchemaWithMetadataResponse(subject, version);
    return extractRawSchemaWithMetadata(subject, version, response);
  }

  @Override
  public Optional<RawSchemaWithMetadata> getRawSchemaWithMetadata(
      TopicName topic, SchemaId schemaId) {
    String subject = subjectNamingStrategy.apply(topic);
    Optional<RawSchema> schema = getRawSchema(subject, schemaId);

    return schema
        .map(sc -> getRawSchemaWithMetadataResponse(subject, sc))
        .map(response -> extractRawSchemaWithMetadata(subject, schemaId, response))
        .map(Optional::get);
  }

  @Override
  public Optional<RawSchemaWithMetadata> getLatestRawSchemaWithMetadata(TopicName topic) {
    final String version = "latest";
    String subject = subjectNamingStrategy.apply(topic);
    Response response = getRawSchemaWithMetadataResponse(subject, version);
    return extractRawSchemaWithMetadata(subject, version, response);
  }

  private Response getRawSchemaWithMetadataResponse(String subject, String version) {
    return schemaRepositoryInstanceResolver
        .resolve(subject)
        .path("subjects")
        .path(subject)
        .path("versions")
        .path(version)
        .request()
        .get();
  }

  private Response getRawSchemaWithMetadataResponse(String subject, RawSchema schema) {
    return schemaRepositoryInstanceResolver
        .resolve(subject)
        .path("subjects")
        .path(subject)
        .request()
        .accept(MediaType.APPLICATION_JSON_TYPE)
        .post(
            Entity.entity(
                SchemaRegistryRequestResponse.fromRawSchema(schema), SCHEMA_REPO_CONTENT_TYPE));
  }

  public Optional<RawSchema> getRawSchema(String subject, SchemaId schemaId) {
    String idString = Integer.toString(schemaId.value());

    Response response =
        schemaRepositoryInstanceResolver
            .resolve(subject)
            .path("schemas")
            .path("ids")
            .path(idString)
            .request()
            .get();

    return extractSchema(response, subject, schemaId);
  }

  private Optional<RawSchema> extractSchema(Response response, String subject, SchemaId schemaId) {
    switch (response.getStatusInfo().getFamily()) {
      case SUCCESSFUL:
        logger.info("Found schema for subject {} and id {}", subject, schemaId.value());
        SchemaRegistryRequestResponse schemaRegistryResponse =
            response.readEntity(SchemaRegistryRequestResponse.class);
        return Optional.of(RawSchema.valueOf(schemaRegistryResponse.getSchema()));
      case CLIENT_ERROR:
        logger.error(
            "Could not find schema for subject {} and id {}, reason: {}",
            subject,
            schemaId.value(),
            response.getStatus());
        return Optional.empty();
      case SERVER_ERROR:
      default:
        logger.error(
            "Could not find schema for subject {} and id {}, reason: {}",
            subject,
            schemaId.value(),
            response.getStatus());
        throw new InternalSchemaRepositoryException(subject, response);
    }
  }

  private Optional<RawSchemaWithMetadata> extractRawSchemaWithMetadata(
      String subject, String version, Response response) {
    switch (response.getStatusInfo().getFamily()) {
      case SUCCESSFUL:
        logger.info("Found schema metadata for subject {} at version {}", subject, version);
        SchemaRegistryResponse schemaRegistryResponse =
            response.readEntity(SchemaRegistryResponse.class);
        return Optional.of(schemaRegistryResponse.toRawSchemaWithMetadata());
      case CLIENT_ERROR:
        logger.error(
            "Could not find schema metadata for subject {} at version {}, reason: {}",
            subject,
            version,
            response.getStatus());
        return Optional.empty();
      case SERVER_ERROR:
      default:
        logger.error(
            "Could not find schema metadata for subject {} at version {}, reason: {}",
            subject,
            version,
            response.getStatus());
        throw new InternalSchemaRepositoryException(subject, response);
    }
  }

  private Optional<RawSchemaWithMetadata> extractRawSchemaWithMetadata(
      String subject, SchemaId schemaId, Response response) {
    Integer id = schemaId.value();
    switch (response.getStatusInfo().getFamily()) {
      case SUCCESSFUL:
        logger.info("Found schema metadata for subject {} and id {}", subject, id);
        SchemaRegistryResponse schemaRegistryResponse =
            response.readEntity(SchemaRegistryResponse.class);
        return Optional.of(schemaRegistryResponse.toRawSchemaWithMetadata());
      case CLIENT_ERROR:
        logger.error(
            "Could not find schema metadata for subject {} and id {}, reason: {}",
            subject,
            id,
            response.getStatus());
        return Optional.empty();
      case SERVER_ERROR:
      default:
        logger.error(
            "Could not find schema metadata for subject {} and id {}, reason: {}",
            subject,
            id,
            response.getStatus());
        throw new InternalSchemaRepositoryException(subject, response);
    }
  }

  @Override
  public List<SchemaVersion> getVersions(TopicName topic) {
    String subject = subjectNamingStrategy.apply(topic);
    Response response =
        schemaRepositoryInstanceResolver
            .resolve(subject)
            .path("subjects")
            .path(subject)
            .path("versions")
            .request()
            .get();
    return extractSchemaVersions(subject, response);
  }

  private List<SchemaVersion> extractSchemaVersions(String subject, Response response) {
    switch (response.getStatusInfo().getFamily()) {
      case SUCCESSFUL:
        return Arrays.stream(response.readEntity(Integer[].class))
            .sorted(Comparator.reverseOrder())
            .map(SchemaVersion::valueOf)
            .collect(Collectors.toList());
      case CLIENT_ERROR:
        logger.error(
            "Could not find schema versions for subject {}, reason: {} {}",
            subject,
            response.getStatus(),
            response.readEntity(String.class));
        return Collections.emptyList();
      case SERVER_ERROR:
      default:
        throw new InternalSchemaRepositoryException(subject, response);
    }
  }

  @Override
  public void registerSchema(TopicName topic, RawSchema rawSchema) {
    String subject = subjectNamingStrategy.apply(topic);
    Response response =
        schemaRepositoryInstanceResolver
            .resolve(subject)
            .path("subjects")
            .path(subject)
            .path("versions")
            .request()
            .accept(MediaType.APPLICATION_JSON_TYPE)
            .post(
                Entity.entity(
                    SchemaRegistryRequestResponse.fromRawSchema(rawSchema),
                    SCHEMA_REPO_CONTENT_TYPE));
    checkSchemaRegistration(subject, response);
  }

  private void checkSchemaRegistration(String subject, Response response) {
    switch (response.getStatusInfo().getFamily()) {
      case SUCCESSFUL:
        logger.info("Successful write to schema registry for subject {}", subject);
        break;
      case CLIENT_ERROR:
        throw new BadSchemaRequestException(subject, response);
      case SERVER_ERROR:
      default:
        throw new InternalSchemaRepositoryException(subject, response);
    }
  }

  @Override
  public void deleteAllSchemaVersions(TopicName topic) {
    String subject = subjectNamingStrategy.apply(topic);
    Response response =
        schemaRepositoryInstanceResolver
            .resolve(subject)
            .path("subjects")
            .path(subject)
            .path(deleteSchemaPathSuffix)
            .request()
            .delete();
    checkSchemaRemoval(subject, response);
  }

  private void checkSchemaRemoval(String subject, Response response) {
    switch (response.getStatusInfo().getFamily()) {
      case SUCCESSFUL:
        logger.info("Successful removed schema subject {}", subject);
        break;
      case CLIENT_ERROR:
        throw new BadSchemaRequestException(subject, response);
      case SERVER_ERROR:
      default:
        int statusCode = response.getStatus();
        String responseBody = response.readEntity(String.class);
        logger.warn(
            "Could not remove schema of subject {}. Reason: {} {}",
            subject,
            statusCode,
            responseBody);
        throw new InternalSchemaRepositoryException(subject, statusCode, responseBody);
    }
  }

  @Override
  public void validateSchema(TopicName topic, RawSchema schema) {
    String subject = subjectNamingStrategy.apply(topic);
    checkCompatibility(subject, schema);
    if (validationEndpointEnabled) {
      checkValidation(subject, schema);
    }
  }

  private void checkCompatibility(String subject, RawSchema schema) {
    Response response =
        schemaRepositoryInstanceResolver
            .resolve(subject)
            .path("compatibility")
            .path("subjects")
            .path(subject)
            .path("versions")
            .path("latest")
            .request()
            .accept(MediaType.APPLICATION_JSON_TYPE)
            .post(
                Entity.entity(
                    SchemaRegistryRequestResponse.fromRawSchema(schema), SCHEMA_REPO_CONTENT_TYPE));
    checkSchemaCompatibilityResponse(subject, response);
  }

  private void checkValidation(String subject, RawSchema schema) {
    Response response =
        schemaRepositoryInstanceResolver
            .resolve(subject)
            .path("subjects")
            .path(subject)
            .path("validation")
            .request()
            .accept(MediaType.APPLICATION_JSON_TYPE)
            .post(
                Entity.entity(
                    SchemaRegistryRequestResponse.fromRawSchema(schema), SCHEMA_REPO_CONTENT_TYPE));

    checkValidationResponse(subject, response);
  }

  private void checkValidationResponse(String subject, Response response) {
    if (response.getStatusInfo().getFamily() == SUCCESSFUL) {
      validateSuccessfulValidationResult(subject, response);
    } else {
      handleErrorResponse(subject, response);
    }
  }

  private void validateSuccessfulValidationResult(String subject, Response response) {
    SchemaRegistryValidationResponse validationResponse =
        response.readEntity(SchemaRegistryValidationResponse.class);

    if (!validationResponse.isValid()) {
      throw new BadSchemaRequestException(
          subject, BAD_REQUEST.getStatusCode(), validationResponse.getErrorsMessage());
    }
  }

  private void checkSchemaCompatibilityResponse(String subject, Response response) {
    if (response.getStatusInfo().getFamily() == SUCCESSFUL) {
      validateSuccessfulCompatibilityResult(subject, response);
    } else {
      handleErrorResponse(subject, response);
    }
  }

  private void handleErrorResponse(String subject, Response response) {
    switch (response.getStatusInfo().getFamily()) {
      case CLIENT_ERROR:
        if (response.getStatus() == 422) { // for other cases we assume the schema is valid
          throw new BadSchemaRequestException(subject, response);
        }
        break;
      case SERVER_ERROR:
      default:
        int statusCode = response.getStatus();
        String responseBody = response.readEntity(String.class);
        logger.warn(
            "Could not validate schema of subject {}. Reason: {} {}",
            subject,
            statusCode,
            responseBody);
        throw new InternalSchemaRepositoryException(subject, statusCode, responseBody);
    }
  }

  private void validateSuccessfulCompatibilityResult(String subject, Response response) {
    String validationResultStr = response.readEntity(String.class);
    SchemaRegistryCompatibilityResponse validationResponse =
        toSchemaRegistryValidationResponse(subject, validationResultStr, response.getStatus());
    if (!validationResponse.isCompatible()) {
      throw new BadSchemaRequestException(subject, response.getStatus(), validationResultStr);
    }
  }

  private SchemaRegistryCompatibilityResponse toSchemaRegistryValidationResponse(
      String subject, String validationResultStr, int status) {
    try {
      return objectMapper.readValue(validationResultStr, SchemaRegistryCompatibilityResponse.class);
    } catch (IOException e) {
      logger.error("Could not parse schema validation response from schema registry", e);
      throw new InternalSchemaRepositoryException(subject, status, validationResultStr);
    }
  }
}
