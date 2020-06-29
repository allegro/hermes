package pl.allegro.tech.hermes.schema.schemarepo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.RawSchema;
import pl.allegro.tech.hermes.api.SchemaWithId;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.schema.*;
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
    public Optional<SchemaWithId> getSchemaWithId(TopicName topic, SchemaVersion version) {
        String subject = subjectNamingStrategy.apply(topic);
        String versionString = Integer.toString(version.value());
        Response response = getSchemaResponse(subject, versionString);
        return extractSchemaWithId(subject, versionString, response);
    }

    @Override
    public Optional<SchemaWithId> getLatestSchemaWithId(TopicName topic) {
        String subject = subjectNamingStrategy.apply(topic);
        Response response = getLatestSchemaResponse(subject);
        return extractSchemaWithId(subject, "latest", response);
    }

    private Optional<SchemaWithId> extractSchemaWithId(String subject, String version, Response response) {
        switch (response.getStatusInfo().getFamily()) {
            case SUCCESSFUL:
                logger.info("Found schema data for subject {} at version {}", subject, version);
                SchemaRepoResponse schemaRepoResponse = response.readEntity(SchemaRepoResponse.class);
                return Optional.of(schemaRepoResponse.toSchemaWithId());
            case CLIENT_ERROR:
                logger.error("Could not find schema data for subject {} at version {}, reason: {}", subject, version, response.getStatus());
                return Optional.empty();
            case SERVER_ERROR:
            default:
                logger.error("Could not find schema data for subject {} at version {}, reason: {}", subject, version, response.getStatus());
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

    private Response getSchemaResponse(String subject, String version) {
        return schemaRepositoryInstanceResolver.resolve(subject)
            .path(subject)
            .path("id")
            .path(version)
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
