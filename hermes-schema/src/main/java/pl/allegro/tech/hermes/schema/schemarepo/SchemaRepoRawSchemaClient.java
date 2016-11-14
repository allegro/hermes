package pl.allegro.tech.hermes.schema.schemarepo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.RawSchema;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.schema.CouldNotFetchSchemaVersionException;
import pl.allegro.tech.hermes.schema.CouldNotFetchSchemaVersionsException;
import pl.allegro.tech.hermes.schema.CouldNotRegisterSchemaException;
import pl.allegro.tech.hermes.schema.CouldNotRegisterSchemaSubjectException;
import pl.allegro.tech.hermes.schema.RawSchemaClient;
import pl.allegro.tech.hermes.schema.SchemaVersion;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;

public class SchemaRepoRawSchemaClient implements RawSchemaClient {

    private static final Logger logger = LoggerFactory.getLogger(SchemaRepoRawSchemaClient.class);

    private final WebTarget target;

    public SchemaRepoRawSchemaClient(Client client, URI schemaRepoServerUri) {
        this.target = client.target(schemaRepoServerUri);
    }

    @Override
    public Optional<RawSchema> getSchema(TopicName topic, SchemaVersion version) {
        String subject = topic.qualifiedName();
        String versionString = Integer.toString(version.value());
        Response response = target.path(subject)
                .path("id")
                .path(versionString)
                .request()
                .get();
        return extractSchema(subject, versionString, response).map(RawSchema::valueOf);
    }

    @Override
    public Optional<RawSchema> getLatestSchema(TopicName topic) {
        String subject = topic.qualifiedName();
        final String version = "latest";
        Response response = target.path(subject)
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
                throw new CouldNotFetchSchemaVersionException(subject, version, response.getStatus(), response.readEntity(String.class));
        }
    }

    @Override
    public List<SchemaVersion> getVersions(TopicName topic) {
        Response response = target.path(topic.qualifiedName())
                .path("all")
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        return extractSchemaVersions(topic.qualifiedName(), response);
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
                throw new CouldNotFetchSchemaVersionsException(subject, response);
        }
    }

    @Override
    public void registerSchema(TopicName topic, RawSchema rawSchema) {
        String topicName = topic.qualifiedName();
        if (!isSubjectRegistered(topicName)) {
            registerSubject(topicName);
        }
        registerSchema(topicName, rawSchema.value());
    }

    private boolean isSubjectRegistered(String subject) {
        return target.path(subject).request().get().getStatus() == Response.Status.OK.getStatusCode();
    }

    private void registerSubject(String subject) {
        Response response = target.path(subject).request().put(Entity.entity("", MediaType.APPLICATION_FORM_URLENCODED_TYPE));
        if (SUCCESSFUL != response.getStatusInfo().getFamily()) {
            throw new CouldNotRegisterSchemaSubjectException(subject, response);
        }
    }

    public void registerSchema(String subject, String schema) {
        Response response = target.path(subject).path("register").request().put(Entity.entity(schema, MediaType.TEXT_PLAIN));
        checkSchemaRegistration(subject, response);
    }

    private void checkSchemaRegistration(String subject, Response response) {
        switch (response.getStatusInfo().getFamily()) {
            case SUCCESSFUL:
                logger.info("Successful write to schema repo for subject {}", subject);
                break;
            case CLIENT_ERROR:
            case SERVER_ERROR:
            default:
                throw new CouldNotRegisterSchemaException(subject, response);
        }
    }

    @Override
    public void deleteAllSchemaVersions(TopicName topic) {
        throw new UnsupportedOperationException("Deleting schemas is not supported by this repository type");
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
