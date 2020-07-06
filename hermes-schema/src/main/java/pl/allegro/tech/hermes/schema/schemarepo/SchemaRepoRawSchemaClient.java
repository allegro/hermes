package pl.allegro.tech.hermes.schema.schemarepo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.RawSchema;
import pl.allegro.tech.hermes.api.RawSchemaWithMetadata;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.schema.RawSchemaClient;
import pl.allegro.tech.hermes.schema.SchemaVersion;
import pl.allegro.tech.hermes.schema.SchemaId;
import pl.allegro.tech.hermes.schema.SubjectNamingStrategy;
import pl.allegro.tech.hermes.schema.InternalSchemaRepositoryException;
import pl.allegro.tech.hermes.schema.BadSchemaRequestException;
import pl.allegro.tech.hermes.schema.resolver.SchemaRepositoryInstanceResolver;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * SchemaRepo doesn't support versions.. We simulate it by set version as id..
 */
public class SchemaRepoRawSchemaClient implements RawSchemaClient {

    private static final Logger logger = LoggerFactory.getLogger(SchemaRepoRawSchemaClient.class);

    private final SchemaRepositoryInstanceResolver schemaRepositoryInstanceResolver;
    private final SubjectNamingStrategy subjectNamingStrategy;

    public SchemaRepoRawSchemaClient(SchemaRepositoryInstanceResolver schemaRepositoryInstanceResolver,
                                     SubjectNamingStrategy subjectNameStrategy) {
        this.schemaRepositoryInstanceResolver = schemaRepositoryInstanceResolver;
        this.subjectNamingStrategy = subjectNameStrategy;
    }
    
    @Override
    public Optional<RawSchemaWithMetadata> getRawSchemaWithMetadata(TopicName topic, SchemaVersion version) {
        String subject = subjectNamingStrategy.apply(topic);
        String idString = Integer.toString(version.value());
        Response response = getSchemaResponse(subject, idString);
        return extractRawSchemaWithMetadata(subject, idString, response);
    }

    @Override
    public Optional<RawSchemaWithMetadata> getLatestRawSchemaWithMetadata(TopicName topic) {
        String subject = subjectNamingStrategy.apply(topic);
        Response response = getLatestSchemaResponse(subject);
        return extractRawSchemaWithMetadata(subject, "latest", response);
    }

    @Override
    public Optional<RawSchemaWithMetadata> getRawSchemaWithMetadata(TopicName topic, SchemaId schemaId) {
        String subject = subjectNamingStrategy.apply(topic);
        String idString = Integer.toString(schemaId.value());
        Response response = getSchemaResponse(subject, idString);
        return extractRawSchemaWithMetadata(subject, idString, response);
    }

    private Optional<RawSchemaWithMetadata> extractRawSchemaWithMetadata(String subject, String schemaId, Response response) {
        switch (response.getStatusInfo().getFamily()) {
            case SUCCESSFUL:
                logger.info("Found schema metadata for subject {} at id {}", subject, schemaId);
                SchemaRepoResponse schemaRepoResponse = response.readEntity(SchemaRepoResponse.class);
                return Optional.of(schemaRepoResponse.toRawSchemaWithMetadata());
            case CLIENT_ERROR:
                logger.error("Could not find schema metadata for subject {} at id {}, reason: {}", subject, schemaId, response.getStatus());
                return Optional.empty();
            case SERVER_ERROR:
            default:
                logger.error("Could not find schema metadata for subject {} at id {}, reason: {}", subject, schemaId, response.getStatus());
                throw new InternalSchemaRepositoryException(subject, response);
        }
    }

    @Override
    public List<SchemaVersion> getVersions(TopicName topic) {
        String subject = subjectNamingStrategy.apply(topic);
        Response response = schemaRepositoryInstanceResolver.resolve(subject)
                .path(subject)
                .path("all")
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        return extractSchemaVersions(subject, response);
    }

    private Response getSchemaResponse(String subject, String id) {
        return schemaRepositoryInstanceResolver.resolve(subject)
            .path(subject)
            .path("id")
            .path(id)
            .request()
            .get();
    }

    private Response getLatestSchemaResponse(String subject) {
        return schemaRepositoryInstanceResolver.resolve(subject)
            .path(subject)
            .path("latest")
            .request()
            .get();
    }

    private List<SchemaVersion> extractSchemaVersions(String subject, Response response) {
        switch (response.getStatusInfo().getFamily()) {
            case SUCCESSFUL:
                List<SchemaRepoResponse> schemasWithIds = Optional.ofNullable(response.readEntity(new GenericType<List<SchemaRepoResponse>>() {}))
                        .orElseGet(Collections::emptyList);
                return schemasWithIds.stream()
                        .map(SchemaRepoResponse::getId)
                        .sorted(Comparator.reverseOrder())
                        .map(SchemaVersion::valueOf)
                        .collect(Collectors.toList());
            case CLIENT_ERROR:
                return Collections.emptyList();
            case SERVER_ERROR:
            default:
                throw new InternalSchemaRepositoryException(subject, response);
        }
    }

    @Override
    public void registerSchema(TopicName topic, RawSchema rawSchema) {
        String subject = subjectNamingStrategy.apply(topic);
        if (!isSubjectRegistered(subject)) {
            registerSubject(subject);
        }
        registerSchema(subject, rawSchema.value());
    }

    private boolean isSubjectRegistered(String subject) {
        return schemaRepositoryInstanceResolver.resolve(subject)
                .path(subject)
                .request()
                .get()
                .getStatus() == Response.Status.OK.getStatusCode();
    }

    private void registerSubject(String subject) {
        Response response = schemaRepositoryInstanceResolver.resolve(subject)
                .path(subject)
                .request()
                .put(Entity.entity("", MediaType.APPLICATION_FORM_URLENCODED_TYPE));
        checkSubjectRegistration(subject, response);
    }

    private void checkSubjectRegistration(String subject, Response response) {
        switch (response.getStatusInfo().getFamily()) {
            case SUCCESSFUL:
                logger.info("Successful registered subject {} in schema repo", subject);
                break;
            case CLIENT_ERROR:
                throw new BadSchemaRequestException(subject, response);
            case SERVER_ERROR:
            default:
                throw new InternalSchemaRepositoryException(subject, response);
        }
    }

    public void registerSchema(String subject, String schema) {
        Response response = schemaRepositoryInstanceResolver.resolve(subject)
                .path(subject)
                .path("register")
                .request()
                .put(Entity.entity(schema, MediaType.TEXT_PLAIN));
        checkSchemaRegistration(subject, response);
    }

    private void checkSchemaRegistration(String subject, Response response) {
        switch (response.getStatusInfo().getFamily()) {
            case SUCCESSFUL:
                logger.info("Successful write to schema repo for subject {}", subject);
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
        throw new UnsupportedOperationException("Deleting schemas is not supported by this repository type");
    }

    @Override
    public void validateSchema(TopicName topic, RawSchema rawSchema) {
        // not implemented, let pass through
    }
}
