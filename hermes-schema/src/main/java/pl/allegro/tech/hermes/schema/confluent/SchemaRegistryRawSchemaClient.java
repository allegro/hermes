package pl.allegro.tech.hermes.schema.confluent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.RawSchema;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.schema.*;
import pl.allegro.tech.hermes.schema.resolver.SchemaRepositoryInstanceResolver;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;

/**
 * This implementation of RawSchemaClient is compatible with Confluent Schema Registry API
 * except for the deleteAllSchemaVersions and validation endpoint which are not fully supported by the Confluent project
 */
public class SchemaRegistryRawSchemaClient implements RawSchemaClient {

    private static final Logger logger = LoggerFactory.getLogger(SchemaRegistryRawSchemaClient.class);

    private static final String SCHEMA_REPO_CONTENT_TYPE = "application/vnd.schemaregistry.v1+json";

    private final SchemaRepositoryInstanceResolver schemaRepositoryInstanceResolver;

    private final ObjectMapper objectMapper;
    private final boolean validationEndpointEnabled;
    private final String deleteSchemaPathSuffix;
    private final SubjectNamingStrategy subjectNamingStrategy;

    public SchemaRegistryRawSchemaClient(SchemaRepositoryInstanceResolver schemaRepositoryInstanceResolver, ObjectMapper objectMapper,
                                         SubjectNamingStrategy subjectNamingStrategy) {
        this(schemaRepositoryInstanceResolver, objectMapper, false, "versions", subjectNamingStrategy);
    }

    public SchemaRegistryRawSchemaClient(SchemaRepositoryInstanceResolver schemaRepositoryInstanceResolver, ObjectMapper objectMapper,
                                         boolean validationEndpointEnabled, String deleteSchemaPathSuffix, SubjectNamingStrategy subjectNamingStrategy) {
        this.schemaRepositoryInstanceResolver = schemaRepositoryInstanceResolver;
        this.validationEndpointEnabled = validationEndpointEnabled;
        this.deleteSchemaPathSuffix = deleteSchemaPathSuffix;
        this.objectMapper = objectMapper;
        this.subjectNamingStrategy = subjectNamingStrategy;
    }

    @Override
    public Optional<RawSchema> getSchema(TopicName topic, SchemaVersion schemaVersion) {
        String version = Integer.toString(schemaVersion.value());
        String subject = subjectNamingStrategy.apply(topic);
        Response response = getSchema(subject, version);
        return extractSchema(subject, version, response).map(RawSchema::valueOf);
    }

    @Override
    public Optional<RawSchema> getLatestSchema(TopicName topic) {
        final String version = "latest";
        String subject = subjectNamingStrategy.apply(topic);
        Response response = getSchema(subject, version);
        return extractSchema(subject, version, response).map(RawSchema::valueOf);
    }

    private Response getSchema(String subject, String version) {
        return schemaRepositoryInstanceResolver.resolve(subject)
                .path("subjects")
                .path(subject)
                .path("versions")
                .path(version)
                .request()
                .get();
    }

    private Optional<String> extractSchema(String subject, String version, Response response) {
        switch (response.getStatusInfo().getFamily()) {
            case SUCCESSFUL:
                logger.info("Found schema for subject {} at version {}", subject,  version);
                String schema = response.readEntity(SchemaRegistryResponse.class).getSchema();
                return Optional.of(schema);
            case CLIENT_ERROR:
                logger.error("Could not find schema for subject {} at version {}, reason: {}", subject, version, response.getStatus());
                return Optional.empty();
            case SERVER_ERROR:
            default:
                logger.error("Could not find schema for subject {} at version {}, reason: {}", subject, version, response.getStatus());
                throw new InternalSchemaRepositoryException(subject, response);
        }
    }

    @Override
    public List<SchemaVersion> getVersions(TopicName topic) {
        String subject = subjectNamingStrategy.apply(topic);
        Response response = schemaRepositoryInstanceResolver.resolve(subject)
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
                logger.error("Could not find schema versions for subject {}, reason: {} {}", subject, response.getStatus(),
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
        Response response = schemaRepositoryInstanceResolver.resolve(subject)
                .path("subjects")
                .path(subject)
                .path("versions")
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(SchemaRegistryRequest.fromRawSchema(rawSchema), SCHEMA_REPO_CONTENT_TYPE));
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
        Response response = schemaRepositoryInstanceResolver.resolve(subject)
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
                logger.warn("Could not remove schema of subject {}. Reason: {} {}", subject, statusCode, responseBody);
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
        Response response = schemaRepositoryInstanceResolver.resolve(subject)
                .path("compatibility")
                .path("subjects")
                .path(subject)
                .path("versions")
                .path("latest")
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(SchemaRegistryRequest.fromRawSchema(schema), SCHEMA_REPO_CONTENT_TYPE));
        checkSchemaCompatibilityResponse(subject, response);
    }

    private void checkValidation(String subject, RawSchema schema) {
        Response response = schemaRepositoryInstanceResolver.resolve(subject)
                .path("subjects")
                .path(subject)
                .path("validation")
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(SchemaRegistryRequest.fromRawSchema(schema), SCHEMA_REPO_CONTENT_TYPE));

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
        SchemaRegistryValidationResponse validationResponse = response.readEntity(SchemaRegistryValidationResponse.class);

        if (!validationResponse.isValid()) {
            throw new BadSchemaRequestException(subject, BAD_REQUEST.getStatusCode(),
                    validationResponse.getErrorsMessage());
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
                logger.warn("Could not validate schema of subject {}. Reason: {} {}", subject, statusCode, responseBody);
                throw new InternalSchemaRepositoryException(subject, statusCode, responseBody);
        }
    }

    private void validateSuccessfulCompatibilityResult(String subject, Response response) {
        String validationResultStr = response.readEntity(String.class);
        SchemaRegistryCompatibilityResponse validationResponse = toSchemaRegistryValidationResponse(subject, validationResultStr, response.getStatus());
        if (!validationResponse.isCompatible()) {
            throw new BadSchemaRequestException(subject, response.getStatus(), validationResultStr);
        }
    }

    private SchemaRegistryCompatibilityResponse toSchemaRegistryValidationResponse(String subject, String validationResultStr, int status) {
        try {
            return objectMapper.readValue(validationResultStr, SchemaRegistryCompatibilityResponse.class);
        } catch (IOException e) {
            logger.error("Could not parse schema validation response from schema registry", e);
            throw new InternalSchemaRepositoryException(subject, status, validationResultStr);
        }
    }
}
