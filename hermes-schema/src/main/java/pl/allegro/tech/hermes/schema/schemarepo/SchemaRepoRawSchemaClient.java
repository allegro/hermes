package pl.allegro.tech.hermes.schema.schemarepo;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.allegro.tech.hermes.api.RawSchema;
import pl.allegro.tech.hermes.api.TopicName;
import pl.allegro.tech.hermes.schema.InvalidSchemaException;
import pl.allegro.tech.hermes.schema.SchemaRepositoryServerException;
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
        Response response = target.path(subject).path("id").path(Integer.toString(version.value())).request().get();
        return extractSchema(subject, response).map(RawSchema::valueOf);
    }

    @Override
    public Optional<RawSchema> getLatestSchema(TopicName topic) {
        String subject = topic.qualifiedName();
        Response response = target.path(subject).path("latest").request().get();
        return extractSchema(subject, response).map(RawSchema::valueOf);
    }

    @Override
    public List<SchemaVersion> getVersions(TopicName topic) {
        Response response = target.path(topic.qualifiedName()).path("all").request().accept(MediaType.APPLICATION_JSON_TYPE).get();
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            List<SchemaWithId> schemasWithIds = Optional.ofNullable(response.readEntity(new GenericType<List<SchemaWithId>>() {}))
                    .orElseGet(Collections::emptyList);
            return schemasWithIds.stream()
                    .map(SchemaWithId::getId)
                    .sorted(Comparator.reverseOrder())
                    .map(SchemaVersion::valueOf)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
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
            logger.error("Failure subject registration in schema repo. Subject: {}, response code: {}, details: {}",
                    subject, response.getStatus(), response.readEntity(String.class));
            throw new SchemaRepositoryServerException("Failure subject registration in schema-repo.");
        }
    }

    public void registerSchema(String subject, String schema) {
        Response response = target.path(subject).path("register").request().put(Entity.entity(schema, MediaType.TEXT_PLAIN));
        checkSchemaRegistration(response.getStatusInfo(), subject, response.readEntity(String.class));
    }

    private void checkSchemaRegistration(Response.StatusType statusType, String subject, String response) {
        switch (statusType.getFamily()) {
            case SUCCESSFUL:
                logger.info("Successful write to schema repo for subject {}", subject);
                break;
            case CLIENT_ERROR:
                logger.warn("Invalid schema for subject {}. Details: {}", subject, response);
                throw new InvalidSchemaException("Invalid schema. Reason: " + response);
            case SERVER_ERROR:
                logger.error("Failure write to schema repo for subject {}. Reason: {}", subject, response);
                throw new SchemaRepositoryServerException("Failure writing to schema-repo. Reason: " + response);
            default:
                logger.error("Unknown response from schema-repo. Subject {}, http status {}, Details: {}",
                        subject, statusType.getStatusCode(), response);
                throw new SchemaRepositoryServerException("Unknown response from schema-repo. Reason: " + response);
        }
    }

    @Override
    public void deleteAllSchemaVersions(TopicName topic) {
        throw new UnsupportedOperationException("Deleting schemas is not supported by this repository type");
    }

    private Optional<String> extractSchema(String subject, Response response) {
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            String schema = parseSchema(response.readEntity(String.class));
            return Optional.of(schema);
        } else {
            logger.error("Could not find schema for subject {}, reason: {}", subject, response.getStatus());
            return Optional.empty();
        }
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
