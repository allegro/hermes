package pl.allegro.tech.hermes.schema.confluent;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.RawSchema;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.schema.BadSchemaRequestException;
import pl.allegro.tech.hermes.schema.InternalSchemaRepositoryException;
import pl.allegro.tech.hermes.schema.RawSchemaAdminClient;
import pl.allegro.tech.hermes.schema.SubjectNamingStrategy;
import pl.allegro.tech.hermes.schema.resolver.SchemaRepositoryInstanceResolver;

import java.io.IOException;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.Family.SUCCESSFUL;

/**
 * This implementation of RawSchemaClient is compatible with Confluent Schema Registry API
 * except for the validation endpoint which is not fully supported by the Confluent project.
 */
public final class SchemaRegistryRawSchemaAdminClient extends SchemaRegistryRawSchemaClient implements RawSchemaAdminClient {

    private static final Logger logger = LoggerFactory.getLogger(SchemaRegistryRawSchemaAdminClient.class);

    private static final String SCHEMA_REPO_CONTENT_TYPE = "application/vnd.schemaregistry.v1+json";
    
    private final ObjectMapper objectMapper;
    private final boolean validationEndpointEnabled;
    private final String deleteSchemaPathSuffix;

    public SchemaRegistryRawSchemaAdminClient(SchemaRepositoryInstanceResolver schemaRepositoryInstanceResolver,
                                              ObjectMapper objectMapper,
                                              SubjectNamingStrategy subjectNamingStrategy) {
        this(schemaRepositoryInstanceResolver, objectMapper, false, "", subjectNamingStrategy);
    }

    public SchemaRegistryRawSchemaAdminClient(SchemaRepositoryInstanceResolver schemaRepositoryInstanceResolver,
                                              ObjectMapper objectMapper,
                                              boolean validationEndpointEnabled,
                                              String deleteSchemaPathSuffix,
                                              SubjectNamingStrategy subjectNamingStrategy) {
        super(schemaRepositoryInstanceResolver, subjectNamingStrategy);
        this.validationEndpointEnabled = validationEndpointEnabled;
        this.deleteSchemaPathSuffix = deleteSchemaPathSuffix;
        this.objectMapper = objectMapper;
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
                .post(Entity.entity(SchemaRegistryRequestResponse.fromRawSchema(rawSchema), SCHEMA_REPO_CONTENT_TYPE));
        checkSchemaRegistration(subject, response);
    }

    private void checkSchemaRegistration(String subject, Response response) {
        switch (response.getStatusInfo().getFamily()) {
            case SUCCESSFUL -> logger.info("Successful write to schema registry for subject {}", subject);
            case CLIENT_ERROR -> throw new BadSchemaRequestException(subject, response);
            default -> throw new InternalSchemaRepositoryException(subject, response);
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
            case SUCCESSFUL -> logger.info("Successful removed schema subject {}", subject);
            case CLIENT_ERROR -> throw new BadSchemaRequestException(subject, response);
            default -> {
                int statusCode = response.getStatus();
                String responseBody = response.readEntity(String.class);
                logger.warn("Could not remove schema of subject {}. Reason: {} {}", subject, statusCode, responseBody);
                throw new InternalSchemaRepositoryException(subject, statusCode, responseBody);
            }
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
                .post(Entity.entity(SchemaRegistryRequestResponse.fromRawSchema(schema), SCHEMA_REPO_CONTENT_TYPE));
        checkSchemaCompatibilityResponse(subject, response);
    }

    private void checkValidation(String subject, RawSchema schema) {
        Response response = schemaRepositoryInstanceResolver.resolve(subject)
                .path("subjects")
                .path(subject)
                .path("validation")
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(SchemaRegistryRequestResponse.fromRawSchema(schema), SCHEMA_REPO_CONTENT_TYPE));

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
            case CLIENT_ERROR -> {
                if (response.getStatus() == 422) { // for other cases we assume the schema is valid
                    throw new BadSchemaRequestException(subject, response);
                }
            }
            default -> {
                int statusCode = response.getStatus();
                String responseBody = response.readEntity(String.class);
                logger.warn("Could not validate schema of subject {}. Reason: {} {}", subject, statusCode, responseBody);
                throw new InternalSchemaRepositoryException(subject, statusCode, responseBody);
            }
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

    private SchemaRegistryCompatibilityResponse toSchemaRegistryValidationResponse(String subject, String validationResultStr, int status) {
        try {
            return objectMapper.readValue(validationResultStr, SchemaRegistryCompatibilityResponse.class);
        } catch (IOException e) {
            logger.error("Could not parse schema validation response from schema registry", e);
            throw new InternalSchemaRepositoryException(subject, status, validationResultStr);
        }
    }
}
