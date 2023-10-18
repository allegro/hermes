package pl.allegro.tech.hermes.schema.confluent;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.RawSchema;
import pl.allegro.tech.hermes.api.RawSchemaWithMetadata;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.schema.InternalSchemaRepositoryException;
import pl.allegro.tech.hermes.schema.RawSchemaClient;
import pl.allegro.tech.hermes.schema.SchemaId;
import pl.allegro.tech.hermes.schema.SchemaVersion;
import pl.allegro.tech.hermes.schema.SubjectNamingStrategy;
import pl.allegro.tech.hermes.schema.resolver.SchemaRepositoryInstanceResolver;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This implementation of RawSchemaClient is compatible with Confluent Schema Registry API.
 */
public class SchemaRegistryRawSchemaClient implements RawSchemaClient {

    private static final Logger logger = LoggerFactory.getLogger(SchemaRegistryRawSchemaClient.class);

    private static final String SCHEMA_REPO_CONTENT_TYPE = "application/vnd.schemaregistry.v1+json";

    protected final SchemaRepositoryInstanceResolver schemaRepositoryInstanceResolver;
    protected final SubjectNamingStrategy subjectNamingStrategy;

    public SchemaRegistryRawSchemaClient(SchemaRepositoryInstanceResolver schemaRepositoryInstanceResolver,
                                         SubjectNamingStrategy subjectNamingStrategy) {
        this.schemaRepositoryInstanceResolver = schemaRepositoryInstanceResolver;
        this.subjectNamingStrategy = subjectNamingStrategy;
    }

    @Override
    public Optional<RawSchemaWithMetadata> getRawSchemaWithMetadata(TopicName topic, SchemaVersion schemaVersion) {
        String version = Integer.toString(schemaVersion.value());
        String subject = subjectNamingStrategy.apply(topic);
        Response response = getRawSchemaWithMetadataResponse(subject, version);
        return extractRawSchemaWithMetadata(subject, version, response);
    }

    @Override
    public Optional<RawSchemaWithMetadata> getRawSchemaWithMetadata(TopicName topic, SchemaId schemaId) {
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
        return schemaRepositoryInstanceResolver.resolve(subject)
                .path("subjects")
                .path(subject)
                .path("versions")
                .path(version)
                .request()
                .get();
    }

    private Response getRawSchemaWithMetadataResponse(String subject, RawSchema schema) {
        return schemaRepositoryInstanceResolver.resolve(subject)
                .path("subjects")
                .path(subject)
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(SchemaRegistryRequestResponse.fromRawSchema(schema), SCHEMA_REPO_CONTENT_TYPE));
    }

    public Optional<RawSchema> getRawSchema(String subject, SchemaId schemaId) {
        String idString = Integer.toString(schemaId.value());

        Response response = schemaRepositoryInstanceResolver.resolve(subject)
                .path("schemas")
                .path("ids")
                .path(idString)
                .request()
                .get();

        return extractSchema(response, subject, schemaId);
    }

    private Optional<RawSchema> extractSchema(Response response, String subject, SchemaId schemaId) {
        switch (response.getStatusInfo().getFamily()) {
            case SUCCESSFUL -> {
                logger.info("Found schema for subject {} and id {}", subject, schemaId.value());
                SchemaRegistryRequestResponse schemaRegistryResponse = response.readEntity(SchemaRegistryRequestResponse.class);
                return Optional.of(RawSchema.valueOf(schemaRegistryResponse.getSchema()));
            }
            case CLIENT_ERROR -> {
                logger.error("Could not find schema for subject {} and id {}, reason: {}", subject, schemaId.value(), response.getStatus());
                return Optional.empty();
            }
            default -> {
                logger.error("Could not find schema for subject {} and id {}, reason: {}", subject, schemaId.value(), response.getStatus());
                throw new InternalSchemaRepositoryException(subject, response);
            }
        }
    }

    private Optional<RawSchemaWithMetadata> extractRawSchemaWithMetadata(String subject, String version, Response response) {
        switch (response.getStatusInfo().getFamily()) {
            case SUCCESSFUL -> {
                logger.info("Found schema metadata for subject {} at version {}", subject, version);
                SchemaRegistryResponse schemaRegistryResponse = response.readEntity(SchemaRegistryResponse.class);
                return Optional.of(schemaRegistryResponse.toRawSchemaWithMetadata());
            }
            case CLIENT_ERROR -> {
                logger.error("Could not find schema metadata for subject {} at version {}, reason: {}",
                             subject,
                             version,
                             response.getStatus());
                return Optional.empty();
            }
            default -> {
                logger.error("Could not find schema metadata for subject {} at version {}, reason: {}",
                             subject,
                             version,
                             response.getStatus());
                throw new InternalSchemaRepositoryException(subject, response);
            }
        }
    }

    private Optional<RawSchemaWithMetadata> extractRawSchemaWithMetadata(String subject, SchemaId schemaId, Response response) {
        Integer id = schemaId.value();
        switch (response.getStatusInfo().getFamily()) {
            case SUCCESSFUL -> {
                logger.info("Found schema metadata for subject {} and id {}", subject, id);
                SchemaRegistryResponse schemaRegistryResponse = response.readEntity(SchemaRegistryResponse.class);
                return Optional.of(schemaRegistryResponse.toRawSchemaWithMetadata());
            }
            case CLIENT_ERROR -> {
                logger.error("Could not find schema metadata for subject {} and id {}, reason: {}", subject, id, response.getStatus());
                return Optional.empty();
            }
            default -> {
                logger.error("Could not find schema metadata for subject {} and id {}, reason: {}", subject, id, response.getStatus());
                throw new InternalSchemaRepositoryException(subject, response);
            }
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
            case SUCCESSFUL -> {
                return Arrays.stream(response.readEntity(Integer[].class))
                    .sorted(Comparator.reverseOrder())
                    .map(SchemaVersion::valueOf)
                    .collect(Collectors.toList());
            }
            case CLIENT_ERROR -> {
                logger.error("Could not find schema versions for subject {}, reason: {} {}", subject, response.getStatus(),
                             response.readEntity(String.class));
                return Collections.emptyList();
            }
            default -> throw new InternalSchemaRepositoryException(subject, response);
        }
    }
}
