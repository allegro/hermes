package pl.allegro.tech.hermes.schema.schemarepo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.RawSchema;
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

    String toSubject(TopicName topic) {
        return topic.qualifiedName();
    }

    @Override
    public Optional<RawSchema> getSchema(TopicName topic, SchemaVersion version) {
        String subject = subjectNamingStrategy.apply(topic);
        String versionString = Integer.toString(version.value());
        Response response = schemaRepositoryInstanceResolver.resolve(subject)
                .path(subject)
                .path("id")
                .path(versionString)
                .request()
                .get();
        return extractSchema(subject, versionString, response).map(RawSchema::valueOf);
    }

    @Override
    public Optional<RawSchema> getLatestSchema(TopicName topic) {
        String subject = subjectNamingStrategy.apply(topic);
        final String version = "latest";
        Response response = schemaRepositoryInstanceResolver.resolve(subject)
                .path(subject)
                .path(version)
                .request()
                .get();
        return extractSchema(subject, version, response).map(RawSchema::valueOf);
    }

    private Optional<String> extractSchema(String subject, String version, Response response) {
        switch (response.getStatusInfo().getFamily()) {
            case SUCCESSFUL:
                String schema = parseSchema(response.readEntity(String.class));
                return Optional.of(schema);
            case CLIENT_ERROR:
                logger.error("Could not find schema for subject {}, reason: {}", subject, response.getStatus());
                return Optional.empty();
            case SERVER_ERROR:
            default:
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

    private List<SchemaVersion> extractSchemaVersions(String subject, Response response) {
        switch (response.getStatusInfo().getFamily()) {
            case SUCCESSFUL:
                List<SchemaWithId> schemasWithIds = Optional.ofNullable(response.readEntity(new GenericType<List<SchemaWithId>>() {}))
                        .orElseGet(Collections::emptyList);
                return schemasWithIds.stream()
                        .map(SchemaWithId::getId)
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

    private String parseSchema(String schemaResponse) {
        return schemaResponse.substring(1 + schemaResponse.indexOf('\t'));
    }

    private static class SchemaWithId {

        private final int id;
        private final String schema;

        @JsonCreator
        SchemaWithId(@JsonProperty("id") int id, @JsonProperty("schema") String schema) {
            this.id = id;
            this.schema = schema;
        }

        int getId() {
            return id;
        }

        String getSchema() {
            return schema;
        }
    }
}
